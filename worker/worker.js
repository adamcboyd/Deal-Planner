// Deal Planner - Cloudflare Worker backend
// Uses Anthropic Claude API. Set ANTHROPIC_API_KEY in Cloudflare Worker environment variables.

const ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
const ANTHROPIC_VERSION = "2023-06-01";
const VISION_MODEL = "claude-sonnet-4-6";
const TEXT_MODEL   = "claude-haiku-4-5-20251001";

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
    if (request.method !== "POST") return new Response("Method not allowed", { status: 405 });
    const url = new URL(request.url);
    const path = url.pathname;
    try {
      const body = await request.json();
      if (path === "/recipes")       return await handleRecipeGeneration(body, env);
      if (path === "/parse")         return await handleVoiceParse(body, env);
      if (path === "/parse-receipt") return await handleReceiptParse(body, env);
      return await handleFlyerScan(body, env);
    } catch (error) {
      return new Response(JSON.stringify({ error: error.message }), {
        status: 500,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
      });
    }
  }
};

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
      { type: "image", source: { type: "base64", media_type: mediaType || "image/jpeg", data: image } },
      { type: "text", text: prompt }
    ]
  };
}
function textMessage(prompt) { return { role: "user", content: prompt }; }

async function handleFlyerScan(body, env) {
  const { image, store, mediaType, ocrText } = body;
  if (!image && !ocrText) return jsonError("No image or OCR text provided", 400);
  const prompt = `You are analyzing a grocery store flyer from ${store || "a grocery store"}. Extract ALL deals/prices.
For each deal: item (specific name), price (number), unit (lb/ea/oz/ct/pk/gal), type (SALE/MEMBER/DIGITAL_COUPON/PAPER_COUPON), confidence (0-1), couponFlag (true/false).
Respond ONLY with a JSON array: [{"item":"Chicken Thighs","price":1.99,"unit":"lb","type":"SALE","confidence":0.95,"couponFlag":false}]
If no prices found return []`;
  try {
    const textContent = image
      ? await callClaude(env, VISION_MODEL, [imageMessage(prompt, image, mediaType)], 4000)
      : await callClaude(env, TEXT_MODEL, [textMessage(`${prompt}\n\nOCR text:\n${ocrText}`)], 4000);
    return jsonOk({ deals: parseJsonPayload(textContent, "array"), success: true });
  } catch (error) { return jsonError(error.message, 500); }
}

async function handleVoiceParse(body, env) {
  const { transcript } = body;
  if (!transcript?.trim()) return jsonError("No transcript provided", 400);
  const prompt = `Convert this pantry voice transcript into structured items.
Transcript: ${transcript}
Return ONLY a JSON array: [{"name":"Black Beans","qty":2,"unit":"can","category":"ANCHOR","expiry":"","brand":"","confidence":0.91}]
Categories: ANCHOR, PROTEIN, PRODUCE, DAIRY, STAPLE. Respond with JSON only.`;
  try {
    const items = parseJsonPayload(
      await callClaude(env, TEXT_MODEL, [textMessage(prompt)]),
      "array"
    ).map(sanitizePantryItem).filter(i => i.name);
    return jsonOk({ items, success: true });
  } catch (error) { return jsonError(error.message, 500); }
}

async function handleReceiptParse(body, env) {
  const { image, mediaType, receiptText } = body;
  if (!image && !receiptText?.trim()) return jsonError("No image or receipt text provided", 400);
  const prompt = `Extract grocery items from this receipt. Return ONLY a JSON array: [{"rawLine":"MILK 3.99","name":"Milk","qty":1,"totalCost":3.99,"confidence":0.92,"brand":""}]
Ignore subtotals, tax, payment lines.`;
  try {
    const textContent = image
      ? await callClaude(env, VISION_MODEL, [imageMessage(prompt, image, mediaType)], 4000)
      : await callClaude(env, TEXT_MODEL, [textMessage(`${prompt}\n\nReceipt:\n${receiptText}`)], 4000);
    const items = parseJsonPayload(textContent, "array")
      .map(sanitizeReceiptItem)
      .filter(i => i.name && i.totalCost > 0);
    return jsonOk({ items, success: true });
  } catch (error) { return jsonError(error.message, 500); }
}

async function handleRecipeGeneration(body, env) {
  const { ingredients, dietType, allergies, gerdFriendly, count, maxComplexity } = body;
  if (!ingredients?.length) return jsonError("No ingredients provided", 400);
  const recipeCount = Math.min(Math.max(Number(count) || 10, 1), 20);
  const prompt = `Generate ${recipeCount} recipes using ONLY: ${ingredients.join(", ")}.
Diet: ${dietType || "omnivore"}. ${allergies?.length ? "AVOID: " + allergies.join(", ") : ""} ${gerdFriendly ? "GERD-friendly (no tomatoes, citrus, garlic, onions, spicy)." : ""} ${maxComplexity ? "Complexity: " + maxComplexity : ""}
At least 2 BREAKFAST, 2 LUNCH, 2 DINNER.
Return ONLY JSON array: [{"name":"Name","type":"DINNER","description":"...","ingredients":["1 lb chicken"],"instructions":"1. ..."}]`;
  try {
    let recipes = parseJsonPayload(
      await callClaude(env, TEXT_MODEL, [textMessage(prompt)], 4000),
      "array"
    );
    recipes = validateGeneratedRecipes(recipes, ingredients, recipeCount);
    return jsonOk({ recipes, success: true });
  } catch (error) { return jsonError(error.message, 500); }
}

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
  try { return JSON.parse(cleaned); } catch {
    const match = expectedType === "array"
      ? cleaned.match(/\[[\s\S]*\]/)
      : cleaned.match(/\{[\s\S]*\}/);
    if (!match) throw new Error("No JSON found in AI response");
    return JSON.parse(match[0]);
  }
}

function sanitizePantryItem(item) {
  return {
    name: toTitleCase(String(item?.name || "").trim()),
    qty: Number(item?.qty) > 0 ? Number(item.qty) : 1,
    unit: String(item?.unit || "").trim(),
    category: normalizeCategory(item?.category),
    expiry: String(item?.expiry || "").trim(),
    brand: toTitleCase(String(item?.brand || "").trim()),
    confidence: Number(item?.confidence) > 0 ? Number(item.confidence) : 0.85
  };
}

function sanitizeReceiptItem(item) {
  return {
    rawLine: String(item?.rawLine || item?.name || "").trim(),
    name: toTitleCase(String(item?.name || "").trim()),
    qty: Number(item?.qty) > 0 ? Number(item.qty) : 1,
    totalCost: Number(item?.totalCost) > 0 ? Number(item.totalCost) : 0,
    confidence: Number(item?.confidence) > 0 ? Number(item.confidence) : 0.85,
    brand: toTitleCase(String(item?.brand || "").trim())
  };
}

function normalizeCategory(c) {
  const v = String(c || "").trim().toUpperCase();
  return ["ANCHOR", "PROTEIN", "PRODUCE", "DAIRY", "STAPLE"].includes(v) ? v : "STAPLE";
}

function toTitleCase(v) {
  return v.split(/\s+/).filter(Boolean)
    .map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
    .join(" ");
}

function validateGeneratedRecipes(recipes, allowed, count) {
  if (!Array.isArray(recipes)) return [];
  const a = allowed.map(normalizeIngredientText);
  return recipes.map(r => sanitizeRecipe(r, a)).filter(Boolean).slice(0, count);
}

function sanitizeRecipe(recipe, allowed) {
  if (!recipe?.name || !Array.isArray(recipe.ingredients) || !recipe.instructions) return null;
  const valid = recipe.ingredients.filter(i => {
    const n = normalizeIngredientText(i);
    return allowed.some(a => n.includes(a) || a.includes(n));
  });
  if (!valid.length) return null;
  const type = String(recipe.type || "").toUpperCase();
  return {
    name: String(recipe.name).trim(),
    type: ["BREAKFAST", "LUNCH", "DINNER"].includes(type) ? type : "DINNER",
    description: String(recipe.description || "").trim(),
    ingredients: valid,
    instructions: String(recipe.instructions).trim()
  };
}

function normalizeIngredientText(v) {
  return String(v || "").toLowerCase()
    .replace(/^[\d\s./]+/, "")
    .replace(/\b(cups?|tbsp?|tsp?|oz|lb|lbs?|can|cans?|bag|bags?|box|boxes?|jar|jars?|bottle|bottles?|head|bunch|large|medium|small|fresh|dried|frozen|canned|boneless|of|pack|count|ct|gallon)\b/g, " ")
    .replace(/[^a-z0-9\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}
