// worker.js - Deal Planner backend using Anthropic Claude API
const ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
const ANTHROPIC_VERSION = "2023-06-01";
const VISION_MODEL = "claude-sonnet-4-6";        // vision: flyer & receipt scanning
const TEXT_MODEL   = "claude-haiku-4-5-20251001"; // text: voice parse & recipe gen

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "POST, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type"
        }
      });
    }

    if (request.method !== "POST") {
      return new Response("Method not allowed", { status: 405 });
    }

    const url = new URL(request.url);
    const path = url.pathname;

    try {
      const body = await request.json();
      if (path === "/recipes")       return await handleRecipeGeneration(body, env);
      if (path === "/parse")         return await handleVoiceParse(body, env);
      if (path === "/parse-receipt") return await handleReceiptParse(body, env);
      return await handleFlyerScan(body, env);
    } catch (error) {
      console.error("Worker error:", error);
      return new Response(JSON.stringify({ error: error.message }), {
        status: 500,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
      });
    }
  }
};

// ─── Shared Claude helpers ─────────────────────────────────────────────────────

async function callClaude(env, model, messages, max_tokens = 2500) {
  const response = await fetch(ANTHROPIC_API_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-api-key": env.ANTHROPIC_API_KEY,
      "anthropic-version": ANTHROPIC_VERSION
    },
    body: JSON.stringify({ model, max_tokens, messages })
  });

  if (!response.ok) {
    const err = await response.text();
    throw new Error(`Claude API error: ${err}`);
  }

  const data = await response.json();
  return data.content?.[0]?.text ?? "";
}

function imageMessage(prompt, image, mediaType) {
  return {
    role: "user",
    content: [
      {
        type: "image",
        source: { type: "base64", media_type: mediaType || "image/jpeg", data: image }
      },
      { type: "text", text: prompt }
    ]
  };
}

function textMessage(prompt) {
  return { role: "user", content: prompt };
}

// ─── Flyer Scanning ────────────────────────────────────────────────────────────

async function handleFlyerScan(body, env) {
  const { image, store, mediaType, ocrText } = body;

  if (!image && !ocrText) {
    return jsonError("No image or OCR text provided", 400);
  }

  const prompt = `You are analyzing a grocery store flyer from ${store || "a grocery store"}. Extract ALL deals/prices you can see.

For each deal identify:
1. item: specific name (e.g. "Boneless Chicken Thighs" not just "Chicken")
2. price: number only (e.g. 1.99)
3. unit: lb, ea, oz, ct, pk, or gal
4. type: SALE, MEMBER (loyalty card required), DIGITAL_COUPON (must clip), or PAPER_COUPON
5. confidence: 0 to 1
6. couponFlag: true if a coupon must be clipped/redeemed

Respond ONLY with a JSON array, no other text. Example:
[
  {"item":"Red Seedless Grapes","price":1.79,"unit":"lb","type":"MEMBER","confidence":0.94,"couponFlag":false},
  {"item":"Hass Avocado","price":0.89,"unit":"ea","type":"DIGITAL_COUPON","confidence":0.88,"couponFlag":true}
]

For BOGO deals include the regular price and append "(BOGO)" to the item name.
If you cannot read any prices clearly, return an empty array: []`;

  try {
    let textContent;
    if (image) {
      textContent = await callClaude(env, VISION_MODEL, [imageMessage(prompt, image, mediaType)], 4000);
    } else {
      textContent = await callClaude(env, TEXT_MODEL, [textMessage(`${prompt}\n\nOCR text:\n${ocrText}`)], 4000);
    }
    const deals = parseJsonPayload(textContent, "array");
    return jsonOk({ deals, success: true });
  } catch (error) {
    return jsonError(error.message, 500);
  }
}

// ─── Voice / Text Pantry Parse ─────────────────────────────────────────────────

async function handleVoiceParse(body, env) {
  const { transcript } = body;

  if (!transcript?.trim()) {
    return jsonError("No transcript provided", 400);
  }

  const prompt = `Convert this pantry voice transcript into structured pantry items.

Transcript:
${transcript}

Return ONLY a JSON array. Each item:
[
  {"name":"Black Beans","qty":2,"unit":"can","category":"ANCHOR","expiry":"","brand":"","confidence":0.91}
]

Rules:
- Split multiple items into separate objects
- Categories: ANCHOR, PROTEIN, PRODUCE, DAIRY, or STAPLE
- qty must be a number
- Units: oz, lb, can, bag, box, jar, bottle, ct, loaf, bunch, head, gallon, pack, tub, block, or ""
- Unknown expiry or brand = ""
- Respond with JSON only`;

  try {
    const textContent = await callClaude(env, TEXT_MODEL, [textMessage(prompt)]);
    const items = parseJsonPayload(textContent, "array")
      .map(sanitizePantryItem)
      .filter(item => item.name);
    return jsonOk({ items, success: true });
  } catch (error) {
    return jsonError(error.message, 500);
  }
}

// ─── Receipt Parse ─────────────────────────────────────────────────────────────

async function handleReceiptParse(body, env) {
  const { image, mediaType, receiptText } = body;

  if (!image && !receiptText?.trim()) {
    return jsonError("No image or receipt text provided", 400);
  }

  const prompt = `Extract grocery items from this receipt. For each item:
- rawLine: original receipt text
- name: clean product name
- qty: quantity (number)
- totalCost: price paid (number)
- confidence: 0 to 1
- brand: brand name or ""

Ignore subtotals, tax, savings, payment lines, and store metadata.

Return ONLY a JSON array:
[{"rawLine":"MILK 3.99","name":"Milk","qty":1,"totalCost":3.99,"confidence":0.92,"brand":""}]`;

  try {
    let textContent;
    if (image) {
      textContent = await callClaude(env, VISION_MODEL, [imageMessage(prompt, image, mediaType)], 4000);
    } else {
      textContent = await callClaude(env, TEXT_MODEL, [textMessage(`${prompt}\n\nReceipt text:\n${receiptText}`)], 4000);
    }
    const items = parseJsonPayload(textContent, "array")
      .map(sanitizeReceiptItem)
      .filter(item => item.name && item.totalCost > 0);
    return jsonOk({ items, success: true });
  } catch (error) {
    return jsonError(error.message, 500);
  }
}

// ─── Recipe Generation ─────────────────────────────────────────────────────────

async function handleRecipeGeneration(body, env) {
  const { ingredients, dietType, allergies, gerdFriendly, count, maxComplexity } = body;

  if (!ingredients?.length) {
    return jsonError("No ingredients provided", 400);
  }

  const recipeCount    = Math.min(Math.max(Number(count) || 10, 1), 20);
  const allergyText    = allergies?.length ? `AVOID these allergens: ${allergies.join(", ")}.` : "";
  const gerdText       = gerdFriendly ? "GERD-friendly: avoid tomatoes, citrus, spicy foods, high-fat foods, garlic, onions, mint." : "";
  const complexityText = maxComplexity ? `Complexity: ${maxComplexity}.` : "";

  const prompt = `Generate ${recipeCount} unique recipes using ONLY these ingredients: ${ingredients.join(", ")}.

Diet: ${dietType || "omnivore"}
${allergyText}
${gerdText}
${complexityText}

Include at least 2 BREAKFAST, 2 LUNCH, and 2 DINNER recipes.

For each recipe:
- name: creative name
- type: BREAKFAST, LUNCH, or DINNER
- description: one sentence
- ingredients: array with quantities (only from the list above)
- instructions: numbered steps

Respond ONLY with a JSON array:
[{"name":"Name","type":"DINNER","description":"...","ingredients":["1 lb chicken","2 cups rice"],"instructions":"1. ...\\n2. ..."}]`;

  try {
    const textContent = await callClaude(env, TEXT_MODEL, [textMessage(prompt)], 4000);
    let recipes = parseJsonPayload(textContent, "array");
    recipes = validateGeneratedRecipes(recipes, ingredients, recipeCount);
    return jsonOk({ recipes, success: true });
  } catch (error) {
    return jsonError(error.message, 500);
  }
}

// ─── Utilities ─────────────────────────────────────────────────────────────────

function jsonOk(data) {
  return new Response(JSON.stringify(data), {
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
  });
}

function jsonError(message, status = 500) {
  return new Response(JSON.stringify({ error: message }), {
    status,
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
  });
}

function parseJsonPayload(rawText, expectedType) {
  const cleaned = rawText.trim().replace(/```json\s*/gi, "").replace(/```\s*/g, "").trim();
  try {
    return JSON.parse(cleaned);
  } catch {
    const match = expectedType === "array"
      ? cleaned.match(/\[[\s\S]*\]/)
      : cleaned.match(/\{[\s\S]*\}/);
    if (!match) throw new Error("No JSON found in AI response");
    return JSON.parse(match[0]);
  }
}

function sanitizePantryItem(item) {
  return {
    name:       toTitleCase(String(item?.name || "").trim()),
    qty:        Number(item?.qty) > 0 ? Number(item.qty) : 1,
    unit:       String(item?.unit || "").trim(),
    category:   normalizeCategory(item?.category),
    expiry:     String(item?.expiry || "").trim(),
    brand:      toTitleCase(String(item?.brand || "").trim()),
    confidence: Number(item?.confidence) > 0 ? Number(item.confidence) : 0.85
  };
}

function sanitizeReceiptItem(item) {
  return {
    rawLine:    String(item?.rawLine || item?.name || "").trim(),
    name:       toTitleCase(String(item?.name || "").trim()),
    qty:        Number(item?.qty) > 0 ? Number(item.qty) : 1,
    totalCost:  Number(item?.totalCost) > 0 ? Number(item.totalCost) : 0,
    confidence: Number(item?.confidence) > 0 ? Number(item.confidence) : 0.85,
    brand:      toTitleCase(String(item?.brand || "").trim())
  };
}

function normalizeCategory(category) {
  const value = String(category || "").trim().toUpperCase();
  return ["ANCHOR", "PROTEIN", "PRODUCE", "DAIRY", "STAPLE"].includes(value) ? value : "STAPLE";
}

function toTitleCase(value) {
  return value.split(/\s+/).filter(Boolean)
    .map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
    .join(" ");
}

function validateGeneratedRecipes(recipes, allowedIngredients, recipeCount) {
  if (!Array.isArray(recipes)) return [];
  const allowed = allowedIngredients.map(normalizeIngredientText);
  return recipes.map(r => sanitizeRecipe(r, allowed)).filter(Boolean).slice(0, recipeCount);
}

function sanitizeRecipe(recipe, allowedIngredients) {
  if (!recipe?.name || !Array.isArray(recipe.ingredients) || !recipe.instructions) return null;
  const validIngredients = recipe.ingredients.filter(ingredient => {
    const n = normalizeIngredientText(ingredient);
    return allowedIngredients.some(a => n.includes(a) || a.includes(n));
  });
  if (validIngredients.length === 0) return null;
  const type = String(recipe.type || "").toUpperCase();
  return {
    name:         String(recipe.name).trim(),
    type:         ["BREAKFAST", "LUNCH", "DINNER"].includes(type) ? type : "DINNER",
    description:  String(recipe.description || "").trim(),
    ingredients:  validIngredients,
    instructions: String(recipe.instructions).trim()
  };
}

function normalizeIngredientText(value) {
  return String(value || "")
    .toLowerCase()
    .replace(/^[\d\s./]+/, "")
    .replace(/\b(cups?|tbsp?|tsp?|oz|lb|lbs?|pound|pounds?|can|cans?|bag|bags?|box|boxes?|jar|jars?|bottle|bottles?|head|heads?|bunch|bunches?|cloves?|slices?|pieces?|large|medium|small|fresh|dried|frozen|canned|boneless|skinless|of|pack|packs|block|tub|count|ct|gallon|gallons?)\b/g, " ")
    .replace(/[^a-z0-9\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}
