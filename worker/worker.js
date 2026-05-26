// worker.js - Updated for current OpenAI models
const OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
const FLYER_SCAN_MODEL = "gpt-4o-mini";
const RECIPE_MODEL = "gpt-4o-mini";
const PARSER_MODEL = "gpt-4o-mini";

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
      if (path === "/recipes") {
        return await handleRecipeGeneration(body, env);
      }

      if (path === "/parse") {
        return await handleVoiceParse(body, env);
      }

      if (path === "/parse-receipt") {
        return await handleReceiptParse(body, env);
      }

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

async function handleFlyerScan(body, env) {
  const { image, store, mediaType, ocrText } = body;

  if (!image && !ocrText) {
    return new Response(JSON.stringify({ error: "No image or OCR text provided" }), {
      status: 400,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    });
  }

  const prompt = `You are analyzing a grocery store flyer from ${store || "a grocery store"}. Extract ALL deals/prices you can see.

For each deal, identify:
1. Item name (be specific, e.g., "Boneless Chicken Thighs" not just "Chicken")
2. Price (as a number, e.g., 1.99)
3. Unit (lb, ea, oz, ct, pk, or gal)
4. Deal type: SALE, MEMBER (if it says "Member Price" or requires loyalty card), DIGITAL_COUPON (if it says "clip" or shows a digital coupon icon), or PAPER_COUPON
5. confidence: a number from 0 to 1
6. couponFlag: true if a coupon must be clipped or redeemed

Respond ONLY with a JSON array, no other text. Example format:
[
  {"item": "Red Seedless Grapes", "price": 1.79, "unit": "lb", "type": "MEMBER", "confidence": 0.94, "couponFlag": false},
  {"item": "Hass Avocado", "price": 0.89, "unit": "ea", "type": "DIGITAL_COUPON", "confidence": 0.88, "couponFlag": true}
]

If you see "Buy 1 Get 1 Free" or "BOGO", include it with the regular price and note "(BOGO)" in the item name.
Extract as many deals as you can find. If you cannot read any prices clearly, return an empty array: []`;

  let textContent;
  try {
    if (image) {
      const response = await fetch(OPENAI_API_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${env.OPENAI_API_KEY}`
        },
        body: JSON.stringify({
          model: FLYER_SCAN_MODEL,
          max_tokens: 4000,
          messages: [
            {
              role: "user",
              content: [
                {
                  type: "image_url",
                  image_url: {
                    url: `data:${mediaType || "image/jpeg"};base64,${image}`
                  }
                },
                {
                  type: "text",
                  text: prompt
                }
              ]
            }
          ]
        })
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`AI API error: ${errorText}`);
      }
      const data = await response.json();
      textContent = extractChatContent(data.choices?.[0]?.message?.content);
    } else {
      const deals = await runJsonArrayPrompt(
        `${prompt}\n\nHere is the OCR text to parse:\n${ocrText}`,
        env,
        PARSER_MODEL
      );
      return new Response(JSON.stringify({ deals, success: true }), {
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
      });
    }
  } catch (error) {
    return new Response(JSON.stringify({ error: "AI API error", details: error.message }), {
      status: 500,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    });
  }

  if (!textContent) {
    return new Response(JSON.stringify({ error: "No response from AI" }), {
      status: 500,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    });
  }

  let deals;
  try {
    deals = parseJsonPayload(textContent, "array");
  } catch (parseError) {
    return new Response(JSON.stringify({ error: "Failed to parse AI response", raw: textContent }), {
      status: 500,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    });
  }

  return new Response(JSON.stringify({ deals, success: true }), {
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
  });
}

async function handleRecipeGeneration(body, env) {
  const { ingredients, dietType, allergies, gerdFriendly, count, maxComplexity } = body;

  if (!ingredients || ingredients.length === 0) {
    return new Response(JSON.stringify({ error: "No ingredients provided" }), {
      status: 400,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    });
  }

  const recipeCount = Math.min(Math.max(Number(count) || 10, 1), 20);
  const allergyText = allergies && allergies.length > 0 ? `AVOID these allergens: ${allergies.join(", ")}.` : "";
  const gerdText = gerdFriendly ? "Make recipes GERD-friendly: avoid acidic foods (tomatoes, citrus), spicy foods, high-fat foods, garlic, onions, and mint." : "";
  const complexityText = maxComplexity ? `Complexity target: ${maxComplexity}. Keep instructions aligned with that level.` : "";

  const prompt = `Generate ${recipeCount} unique recipes using ONLY these available ingredients: ${ingredients.join(", ")}.

Diet type: ${dietType || "omnivore"}
${allergyText}
${gerdText}
${complexityText}

Create a variety of BREAKFAST, LUNCH, and DINNER recipes (at least 2 of each type).

For each recipe, provide:
- name: Creative, appetizing name
- type: BREAKFAST, LUNCH, or DINNER
- description: One sentence describing the dish
- ingredients: Array of ingredients with quantities (only use items from the available list above)
- instructions: Step-by-step cooking instructions

Respond ONLY with a JSON array, no other text:
[
  {
    "name": "Recipe Name",
    "type": "DINNER",
    "description": "A delicious one-pot meal",
    "ingredients": ["1 lb chicken thighs", "2 cups rice", "1 cup broccoli"],
    "instructions": "1. Season chicken...\\n2. Cook rice...\\n3. Steam broccoli..."
  }
]`;

  const response = await fetch(OPENAI_API_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${env.OPENAI_API_KEY}`
    },
    body: JSON.stringify({
      model: RECIPE_MODEL,
      max_tokens: 4000,
      messages: [
        {
          role: "user",
          content: prompt
        }
      ]
    })
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error("OpenAI API error:", errorText);
    return new Response(JSON.stringify({ error: "AI API error", details: errorText }), {
      status: 500,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    });
  }

  const data = await response.json();
  const textContent = extractChatContent(data.choices?.[0]?.message?.content);

  if (!textContent) {
    return new Response(JSON.stringify({ error: "No response from AI" }), {
      status: 500,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    });
  }

  let recipes;
  try {
    recipes = parseJsonPayload(textContent, "array");
    recipes = validateGeneratedRecipes(recipes, ingredients, recipeCount);
  } catch (parseError) {
    return new Response(JSON.stringify({ error: "Failed to parse AI response", raw: textContent }), {
      status: 500,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    });
  }

  return new Response(JSON.stringify({ recipes, success: true }), {
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
  });
}

async function handleVoiceParse(body, env) {
  const { transcript } = body;

  if (!transcript || !transcript.trim()) {
    return new Response(JSON.stringify({ error: "No transcript provided" }), {
      status: 400,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    });
  }

  const prompt = `Convert this pantry voice transcript into structured pantry items.

Transcript:
${transcript}

Return ONLY a JSON array. Each item must use this shape:
[
  {
    "name": "Black Beans",
    "qty": 2,
    "unit": "can",
    "category": "ANCHOR",
    "expiry": "",
    "brand": "",
    "confidence": 0.91
  }
]

Rules:
- Split multiple items into separate objects.
- Categories must be one of: ANCHOR, PROTEIN, PRODUCE, DAIRY, STAPLE.
- Use qty as a number.
- Use simple units like oz, lb, can, bag, box, jar, bottle, ct, loaf, bunch, head, gallon, pack, tub, block, count, or empty string.
- If expiry is unknown, use "".
- If brand is unknown, use "".
- Include confidence from 0 to 1.
- Respond with JSON only.`;

  const items = await runJsonArrayPrompt(prompt, env, PARSER_MODEL);
  const sanitizedItems = items
    .map(sanitizePantryItem)
    .filter((item) => item.name);

  return new Response(JSON.stringify({ items: sanitizedItems, success: true }), {
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
  });
}

async function handleReceiptParse(body, env) {
  const { receiptText } = body;

  if (!receiptText || !receiptText.trim()) {
    return new Response(JSON.stringify({ error: "No receipt text provided" }), {
      status: 400,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    });
  }

  const prompt = `Extract grocery items from this receipt OCR text.

Receipt text:
${receiptText}

Return ONLY a JSON array in this format:
[
  {
    "rawLine": "MILK 3.99",
    "name": "Milk",
    "qty": 1,
    "totalCost": 3.99,
    "confidence": 0.92,
    "brand": ""
  }
]

Rules:
- Ignore subtotal, tax, savings, coupons, membership lines, payment lines, store metadata, and other non-item text.
- Combine duplicate item lines when obvious.
- Use qty as a number.
- Include rawLine when possible.
- Include totalCost as the final line price.
- Include confidence from 0 to 1.
- Respond with JSON only.`;

  const items = await runJsonArrayPrompt(prompt, env, PARSER_MODEL);
  const sanitizedItems = items
    .map(sanitizeReceiptItem)
    .filter((item) => item.name && item.totalCost > 0);

  return new Response(JSON.stringify({ items: sanitizedItems, success: true }), {
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
  });
}

async function runJsonArrayPrompt(prompt, env, model) {
  const response = await fetch(OPENAI_API_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${env.OPENAI_API_KEY}`
    },
    body: JSON.stringify({
      model,
      max_tokens: 2500,
      messages: [
        {
          role: "user",
          content: prompt
        }
      ]
    })
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`AI API error: ${errorText}`);
  }

  const data = await response.json();
  const textContent = extractChatContent(data.choices?.[0]?.message?.content);

  if (!textContent) {
    throw new Error("No response from AI");
  }

  return parseJsonPayload(textContent, "array");
}

function extractChatContent(content) {
  if (typeof content === "string") {
    return content;
  }

  if (Array.isArray(content)) {
    return content
      .map((part) => {
        if (typeof part === "string") {
          return part;
        }

        if (part?.type === "text") {
          return part.text || "";
        }

        return "";
      })
      .join("\n")
      .trim();
  }

  return "";
}

function parseJsonPayload(rawText, expectedType) {
  const cleaned = rawText
    .trim()
    .replace(/```json\s*/gi, "")
    .replace(/```\s*/g, "")
    .trim();

  try {
    return JSON.parse(cleaned);
  } catch {
    const match =
      expectedType === "array"
        ? cleaned.match(/\[[\s\S]*\]/)
        : cleaned.match(/\{[\s\S]*\}/);

    if (!match) {
      throw new Error("No JSON payload found");
    }

    return JSON.parse(match[0]);
  }
}

function sanitizePantryItem(item) {
  const normalizedCategory = normalizeCategory(item?.category);
  return {
    name: toTitleCase(String(item?.name || "").trim()),
    qty: Number(item?.qty) > 0 ? Number(item.qty) : 1,
    unit: String(item?.unit || "").trim(),
    category: normalizedCategory,
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

function normalizeCategory(category) {
  const value = String(category || "").trim().toUpperCase();
  const allowed = new Set(["ANCHOR", "PROTEIN", "PRODUCE", "DAIRY", "STAPLE"]);
  return allowed.has(value) ? value : "STAPLE";
}

function toTitleCase(value) {
  return value
    .split(/\s+/)
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(" ");
}

function validateGeneratedRecipes(recipes, allowedIngredients, recipeCount) {
  if (!Array.isArray(recipes)) {
    return [];
  }

  const allowed = allowedIngredients.map(normalizeIngredientText);
  const validated = recipes
    .map((recipe) => sanitizeRecipe(recipe, allowed))
    .filter(Boolean);

  return validated.slice(0, recipeCount);
}

function sanitizeRecipe(recipe, allowedIngredients) {
  if (!recipe || !recipe.name || !Array.isArray(recipe.ingredients) || !recipe.instructions) {
    return null;
  }

  const validIngredients = recipe.ingredients.filter((ingredient) => {
    const normalized = normalizeIngredientText(ingredient);
    return allowedIngredients.some((allowed) => normalized.includes(allowed) || allowed.includes(normalized));
  });

  if (validIngredients.length === 0) {
    return null;
  }

  const type = String(recipe.type || "").toUpperCase();
  const normalizedType = ["BREAKFAST", "LUNCH", "DINNER"].includes(type) ? type : "DINNER";

  return {
    name: String(recipe.name).trim(),
    type: normalizedType,
    description: String(recipe.description || "").trim(),
    ingredients: validIngredients,
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
