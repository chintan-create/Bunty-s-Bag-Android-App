package com.example.data.catalog

import androidx.annotation.DrawableRes
import com.example.R
import com.example.data.model.KutchiProduct
import com.example.data.model.ProductCategory

object KutchiCatalog {

    val products: List<KutchiProduct> = listOf(
        KutchiProduct(
            id = "rabari_abhla_tote",
            name = "Rabari Abhla Mirror Tote",
            subtitle = "Hand-stitched diamond mirror work & braided tassels",
            category = ProductCategory.TOTES,
            artisanVillage = "Hodka, Kutch",
            artisanName = "Lachhuben Rabari",
            craftType = "Rabari Embroidery",
            priceInr = 2450L,
            originalPriceInr = 2900L,
            rating = 4.9,
            reviewCount = 84,
            dimensions = "38 cm × 34 cm × 12 cm",
            materials = "Handwoven Kala cotton, real glass abhla mirrors, merino wool pom-poms",
            careInstructions = "Spot clean gently with damp cloth. Keep mirrors away from harsh abrasion.",
            story = "Crafted over 18 days in Hodka village using classic Rabari chain-stitch motifs symbolizing desert stars and desert blooms.",
            variants = listOf("Sindoor Crimson", "Desert Saffron", "Indigo Night"),
            drawableKey = "img_bag_mirror_tote",
            featuredBadge = "Bestseller"
        ),
        KutchiProduct(
            id = "ajrakh_indigo_sling",
            name = "Ajrakhpur Indigo Crossbody",
            subtitle = "Double-sided woodblock printed khadi with saddle leather",
            category = ProductCategory.SLINGS,
            artisanVillage = "Ajrakhpur, Kutch",
            artisanName = "Ismail Khatri Studio",
            craftType = "Ajrakh Block Print",
            priceInr = 1890L,
            originalPriceInr = 2250L,
            rating = 4.8,
            reviewCount = 62,
            dimensions = "25 cm × 19 cm × 8 cm",
            materials = "Natural fermented indigo khadi, madder root dye, vegetable-tanned leather strap",
            careInstructions = "Air dry in shade. Wipe leather trim with dry soft cotton cloth.",
            story = "Each panel undergoes a 14-stage resist-dyeing and hand-carved teak block printing process using river water and mineral mordants.",
            variants = listOf("Indigo Madder", "Charcoal Slate", "Earth Ochre"),
            drawableKey = "img_bag_ajrakh_sling",
            featuredBadge = "Heritage Craft"
        ),
        KutchiProduct(
            id = "mutwa_saffron_potli",
            name = "Mutwa Silk & Mirror Potli",
            subtitle = "Fine geometric needlework with pearl-beaded drawstrings",
            category = ProductCategory.POTLIS,
            artisanVillage = "Dhordo, Banni",
            artisanName = "Fatimaben Mutwa",
            craftType = "Mutwa Embroidery",
            priceInr = 1650L,
            originalPriceInr = 1950L,
            rating = 4.9,
            reviewCount = 47,
            dimensions = "22 cm × 21 cm × 10 cm",
            materials = "Mashru silk-cotton blend, miniature shisha mirrors, zari braid",
            careInstructions = "Store flat inside muslin dust bag included with your parcel.",
            story = "Famous for microscopic stitches set so tightly they resemble woven tapestry, paired with festive Banni tassels.",
            variants = listOf("Marigold & Maroon", "Peacock Teal", "Sindoor Gold"),
            drawableKey = "img_potli_bandhani",
            featuredBadge = "Festive Edit"
        ),
        KutchiProduct(
            id = "bhujodi_kala_tote",
            name = "Bhujodi Extra-Weft Loom Tote",
            subtitle = "Rain-fed organic Kala cotton woven on traditional pit looms",
            category = ProductCategory.TOTES,
            artisanVillage = "Bhujodi, Kutch",
            artisanName = "Vankar Shamji Collective",
            craftType = "Bhujodi Handloom",
            priceInr = 2150L,
            originalPriceInr = 2500L,
            rating = 4.7,
            reviewCount = 39,
            dimensions = "40 cm × 36 cm × 13 cm",
            materials = "100% unbleached organic Kala cotton, natural lac-dyed extra-weft motifs",
            careInstructions = "Gentle cold hand wash with mild soap; dry flat in shade.",
            story = "Woven from indigenous rain-fed Kutch cotton requiring zero synthetic pesticides, accented with traditional Dhabla borders.",
            variants = listOf("Raw Khadi & Crimson", "Indigo Weft", "Saffron Border"),
            drawableKey = "img_bag_mirror_tote"
        ),
        KutchiProduct(
            id = "bandhani_dholavira_clutch",
            name = "Khatri Bandhani Envelope Clutch",
            subtitle = "Micro-knotted tie-dye silk with brass magnetic clasp",
            category = ProductCategory.POTLIS,
            artisanVillage = "Bhuj, Kutch",
            artisanName = "Abdulbhai Khatri",
            craftType = "Kutchi Bandhani",
            priceInr = 1490L,
            originalPriceInr = 1790L,
            rating = 4.8,
            reviewCount = 53,
            dimensions = "26 cm × 15 cm × 5 cm",
            materials = "Gajji silk, cotton lining, hand-tied ekdali dots, antique brass frame",
            careInstructions = "Dry clean only to preserve crisp Bandhani texture.",
            story = "Over 2,800 tiny hand-tied knots create shimmering geometric constellations inspired by the Rann of Kutch night sky.",
            variants = listOf("Crimson Ekdali", "Saffron Sun", "Royal Indigo"),
            drawableKey = "img_potli_bandhani"
        ),
        KutchiProduct(
            id = "dhamadka_madder_sling",
            name = "Dhamadka Madder Messenger Bag",
            subtitle = "Roomy everyday satchel with padded laptop sleeve",
            category = ProductCategory.SLINGS,
            artisanVillage = "Dhamadka, Kutch",
            artisanName = "Razaq Khatri",
            craftType = "Ajrakh & Khadi",
            priceInr = 2690L,
            originalPriceInr = 3100L,
            rating = 4.9,
            reviewCount = 31,
            dimensions = "34 cm × 26 cm × 10 cm",
            materials = "Heavy canvas khadi, Ajrakh flap, antique brass buckles, cotton webbing",
            careInstructions = "Brush away dry dust; spot clean fabric panels as needed.",
            story = "Built for modern creatives and travelers who love heritage textiles with space for a 13-inch laptop and sketchbook.",
            variants = listOf("Madder Rust", "Deep Indigo"),
            drawableKey = "img_bag_ajrakh_sling",
            featuredBadge = "Traveler's Pick"
        ),
        KutchiProduct(
            id = "ahirl_festival_shoulder",
            name = "Ahir Phulkari Shoulder Bag",
            subtitle = "Bold floral chain-stitch embroidery with cotton sling",
            category = ProductCategory.TOTES,
            artisanVillage = "Dhaneti, Kutch",
            artisanName = "Mankuben Ahir",
            craftType = "Ahir Embroidery",
            priceInr = 2290L,
            originalPriceInr = 2650L,
            rating = 4.8,
            reviewCount = 44,
            dimensions = "33 cm × 29 cm × 11 cm",
            materials = "Dyed cotton twill, silk-sheen embroidery threads, round abhla mirrors",
            careInstructions = "Spot clean only; avoid soaking.",
            story = "Celebrates joyful Ahir peacock and marigold patterns stitched along the edges of each roomy compartment.",
            variants = listOf("Crimson Blossom", "Turquoise Peacock"),
            drawableKey = "img_bag_mirror_tote"
        ),
        KutchiProduct(
            id = "nirona_copper_bell_charm",
            name = "Nirona Lohar Copper Bell Bag Charm",
            subtitle = "Hand-forged resonant desert bell with woollen tassel",
            category = ProductCategory.CRAFTS,
            artisanVillage = "Nirona, Kutch",
            artisanName = "Umarbhai Lohar",
            craftType = "Copper Bell Craft",
            priceInr = 680L,
            originalPriceInr = 850L,
            rating = 4.9,
            reviewCount = 96,
            dimensions = "16 cm length × 5 cm bell",
            materials = "Recycled iron sheet coated in brass-copper alloy, hand-dyed wool tassel",
            careInstructions = "Wipe metal lightly with dry cloth; every bell is tuned by ear.",
            story = "Crafted without welding using interlocking folds and clay-fired copper coating—the same melodic bells worn by Maldhari herds.",
            variants = listOf("Sindoor Tassel", "Saffron Tassel", "Indigo Tassel"),
            drawableKey = "img_potli_bandhani",
            featuredBadge = "Artisan Gift"
        ),
        KutchiProduct(
            id = "bhujodi_heritage_stole",
            name = "Bhujodi Tangaliya Woven Stole",
            subtitle = "Hand-twisted dot-weave shawl in organic wool-cotton",
            category = ProductCategory.CRAFTS,
            artisanVillage = "Bhujodi, Kutch",
            artisanName = "Devjibhai Vankar",
            craftType = "Tangaliya Weaving",
            priceInr = 1750L,
            originalPriceInr = 2100L,
            rating = 4.8,
            reviewCount = 28,
            dimensions = "210 cm × 65 cm",
            materials = "Organic Kala cotton & indigenous sheep wool, raised bead-like dots",
            careInstructions = "Gentle cold hand wash separately; dry flat.",
            story = "Recognized with a Geographical Indication tag, Tangaliya weaving twists tiny contrasting threads around warp yarns by hand.",
            variants = listOf("Crimson & Ivory", "Indigo & Saffron"),
            drawableKey = "img_hero_kutchi"
        ),
        KutchiProduct(
            id = "kutch_rogan_travel_pouch",
            name = "Nirona Rogan Painted Travel Pouch",
            subtitle = "Tree-of-Life castor-oil pigment motif on dark cotton",
            category = ProductCategory.CRAFTS,
            artisanVillage = "Nirona, Kutch",
            artisanName = "Khatri Heritage Workshop",
            craftType = "Rogan Art",
            priceInr = 1250L,
            originalPriceInr = 1500L,
            rating = 4.9,
            reviewCount = 41,
            dimensions = "23 cm × 15 cm × 6 cm",
            materials = "Heavy cotton canvas, natural boiled castor-oil Rogan paste, brass zip",
            careInstructions = "Wipe gently with dry cloth; avoid folding directly across painted crest.",
            story = "Painted freehand with an iron stylus using vibrant castor-oil paste folded symmetrically across fabric.",
            variants = listOf("Midnight Tree of Life", "Maroon Floral"),
            drawableKey = "img_bag_ajrakh_sling"
        )
    )

    fun findById(productId: String): KutchiProduct? = products.firstOrNull { it.id == productId }

    @DrawableRes
    fun resolveDrawable(drawableKey: String): Int = when (drawableKey) {
        "img_bag_mirror_tote" -> R.drawable.img_bag_mirror_tote
        "img_bag_ajrakh_sling" -> R.drawable.img_bag_ajrakh_sling
        "img_potli_bandhani" -> R.drawable.img_potli_bandhani
        "img_hero_kutchi" -> R.drawable.img_hero_kutchi
        else -> R.drawable.img_bag_mirror_tote
    }

    fun formatInr(amount: Long): String {
        val str = amount.toString()
        if (str.length <= 3) return "₹$str"
        val lastThree = str.takeLast(3)
        val rest = str.dropLast(3)
        val grouped = rest.reversed().chunked(2).joinToString(",").reversed()
        return "₹$grouped,$lastThree"
    }
}
