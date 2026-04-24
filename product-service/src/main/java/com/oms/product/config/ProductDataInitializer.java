package com.oms.product.config;

import com.oms.product.domain.Product;
import com.oms.product.inventory.domain.Inventory;
import com.oms.product.inventory.repository.InventoryRepository;
import com.oms.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductDataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    private static final Random RANDOM = new Random(42);

    @Override
    public void run(ApplicationArguments args) {
        List<Product> products;
        if (productRepository.count() < 100) {
            products = productRepository.saveAll(seedProducts());
            log.info("Seeded {} products into PostgreSQL", products.size());
        } else {
            log.info("Products already seeded — skipping initialization");
            products = productRepository.findAll();
        }

        if (inventoryRepository.count() < 100) {
            List<Inventory> records = products.stream()
                .filter(p -> !inventoryRepository.existsById(p.getId()))
                .map(p -> Inventory.builder()
                    .productId(p.getId())
                    .availableQty(10 + RANDOM.nextInt(491))
                    .reservedQty(0)
                    .build())
                .toList();
            if (!records.isEmpty()) {
                inventoryRepository.saveAll(records);
                log.info("Seeded {} inventory records", records.size());
            }
        } else {
            log.info("Inventory already seeded — skipping initialization");
        }
    }

    private static Product p(String name, String description, String category,
                              String price, int stock, String imageUrl) {
        return Product.builder()
                .name(name).description(description).category(category)
                .price(new BigDecimal(price)).stockQty(stock).imageUrl(imageUrl)
                .build();
    }

    private static List<Product> seedProducts() {
        return List.of(
            // ── Electronics (20) ──────────────────────────────────────────────
            p("MacBook Pro 16\"", "Apple M3 Pro, 18 GB RAM, 512 GB SSD, Liquid Retina XDR", "Electronics", "2499.99", 25, "https://images.example.com/macbook-pro-16.jpg"),
            p("iPhone 15 Pro", "A17 Pro chip, 256 GB, titanium frame, ProMotion display", "Electronics", "1199.99", 50, "https://images.example.com/iphone-15-pro.jpg"),
            p("Samsung Galaxy S24 Ultra", "Snapdragon 8 Gen 3, 12 GB RAM, 512 GB, 200 MP camera", "Electronics", "1299.99", 40, "https://images.example.com/s24-ultra.jpg"),
            p("Sony WH-1000XM5 Headphones", "Industry-leading noise cancellation, 30-hour battery, wireless", "Electronics", "349.99", 80, "https://images.example.com/sony-wh1000xm5.jpg"),
            p("iPad Pro 12.9\"", "M2 chip, 256 GB, Liquid Retina XDR, Wi-Fi + Cellular", "Electronics", "1099.99", 35, "https://images.example.com/ipad-pro.jpg"),
            p("Dell XPS 15", "Intel Core i7-13700H, 32 GB DDR5, 1 TB SSD, OLED touch display", "Electronics", "1899.99", 20, "https://images.example.com/dell-xps-15.jpg"),
            p("Apple Watch Series 9", "GPS + Cellular, 45 mm, blood oxygen & ECG sensors", "Electronics", "499.99", 60, "https://images.example.com/apple-watch-9.jpg"),
            p("Sony A7 IV Camera", "33 MP full-frame mirrorless, 4K 60fps, dual card slots", "Electronics", "2499.99", 15, "https://images.example.com/sony-a7iv.jpg"),
            p("LG OLED C3 65\" TV", "4K OLED evo, 120 Hz, Dolby Vision, webOS 23, perfect blacks", "Electronics", "1799.99", 10, "https://images.example.com/lg-oled-c3.jpg"),
            p("Nintendo Switch OLED", "7-inch OLED screen, 64 GB storage, enhanced audio", "Electronics", "349.99", 75, "https://images.example.com/switch-oled.jpg"),
            p("Canon EOS R6 Mark II", "24.2 MP full-frame, in-body stabilization, dual-pixel AF", "Electronics", "2499.99", 18, "https://images.example.com/canon-r6-mark2.jpg"),
            p("Logitech MX Master 3S", "8K DPI, quiet clicks, MagSpeed scroll, ergonomic wireless mouse", "Electronics", "99.99", 150, "https://images.example.com/mx-master-3s.jpg"),
            p("Samsung 27\" 4K Monitor", "IPS, 144 Hz, HDR600, USB-C 90 W charging, 99% sRGB", "Electronics", "599.99", 45, "https://images.example.com/samsung-4k-monitor.jpg"),
            p("Anker 20000 mAh Power Bank", "PD 65 W, dual USB-C, LED display, airline-safe", "Electronics", "59.99", 200, "https://images.example.com/anker-powerbank.jpg"),
            p("Razer BlackWidow V4 Keyboard", "Mechanical, Razer Green switches, per-key RGB, wrist rest", "Electronics", "139.99", 85, "https://images.example.com/razer-blackwidow.jpg"),
            p("GoPro Hero 12 Black", "5.3K60 video, HyperSmooth 6.0, waterproof to 10 m, HDR", "Electronics", "399.99", 55, "https://images.example.com/gopro-hero12.jpg"),
            p("Amazon Echo Dot 5th Gen", "Smart speaker with Alexa, improved audio, temperature sensor", "Electronics", "49.99", 300, "https://images.example.com/echo-dot-5.jpg"),
            p("Jabra Elite 85t", "True wireless earbuds, ANC, 5.5-hour battery, USB-C", "Electronics", "229.99", 70, "https://images.example.com/jabra-elite-85t.jpg"),
            p("TP-Link WiFi 6 Router AX5400", "Dual-band, 8 streams, WPA3, works with Alexa", "Electronics", "149.99", 100, "https://images.example.com/tplink-ax5400.jpg"),
            p("Bose QuietComfort 45", "Wireless Bluetooth, noise cancelling, 24-hour battery", "Electronics", "279.99", 90, "https://images.example.com/bose-qc45.jpg"),

            // ── Clothing & Fashion (15) ───────────────────────────────────────
            p("Levi's 501 Original Jeans", "Classic straight-fit denim, 100% cotton, mid-rise", "Clothing", "69.99", 120, "https://images.example.com/levis-501.jpg"),
            p("Nike Air Force 1 Low", "Iconic leather upper, Air-sole cushioning, white/white", "Clothing", "109.99", 95, "https://images.example.com/nike-af1.jpg"),
            p("The North Face Thermoball Jacket", "Synthetic insulation, packable, water-repellent, PrimaLoft", "Clothing", "199.99", 60, "https://images.example.com/tnf-thermoball.jpg"),
            p("Patagonia Nano Puff Vest", "PrimaLoft Gold insulation, recycled nylon, windproof", "Clothing", "149.99", 45, "https://images.example.com/patagonia-nano-puff.jpg"),
            p("Adidas Ultraboost 23", "Boost midsole, Primeknit+ upper, Continental rubber outsole", "Clothing", "189.99", 80, "https://images.example.com/adidas-ultraboost-23.jpg"),
            p("Uniqlo Ultra Light Down Jacket", "90% down fill, packable into own pocket, machine washable", "Clothing", "99.99", 150, "https://images.example.com/uniqlo-ultralight.jpg"),
            p("Calvin Klein Men's Boxer Briefs (3-Pack)", "100% cotton, moisture-wicking, elastic waistband", "Clothing", "39.99", 250, "https://images.example.com/ck-boxer.jpg"),
            p("Carhartt WIP Canvas Work Pants", "Durable canvas fabric, triple-stitched seams, relaxed fit", "Clothing", "89.99", 75, "https://images.example.com/carhartt-canvas.jpg"),
            p("Ralph Lauren Polo Shirt", "100% Pima cotton, embroidered pony logo, slim fit", "Clothing", "79.99", 110, "https://images.example.com/polo-shirt.jpg"),
            p("Timberland 6\" Premium Boots", "Full-grain leather, waterproof, EVA footbed, lug sole", "Clothing", "198.99", 55, "https://images.example.com/timberland-6inch.jpg"),
            p("Champion Reverse Weave Hoodie", "Heavyweight fleece, ribbed cuffs, kangaroo pocket", "Clothing", "59.99", 130, "https://images.example.com/champion-hoodie.jpg"),
            p("Lululemon Align Leggings", "Buttery-soft Nulu fabric, 28\", 4-way stretch, inseam pockets", "Clothing", "98.00", 100, "https://images.example.com/lululemon-align.jpg"),
            p("Columbia PFG Tamiami Shirt", "UPF 40+, moisture-wicking, roll-up sleeves, vented back", "Clothing", "49.99", 85, "https://images.example.com/columbia-pfg.jpg"),
            p("Converse Chuck Taylor All Star", "Canvas upper, vulcanized rubber sole, iconic ankle patch", "Clothing", "59.99", 160, "https://images.example.com/converse-chuck.jpg"),
            p("Arc'teryx Gamma Softshell Pants", "Fortius 2.0 fabric, articulated patterning, DWR finish", "Clothing", "249.99", 30, "https://images.example.com/arcteryx-gamma.jpg"),

            // ── Books (15) ───────────────────────────────────────────────────
            p("Atomic Habits", "James Clear — proven framework for building good habits and breaking bad ones", "Books", "16.99", 500, "https://images.example.com/atomic-habits.jpg"),
            p("The Pragmatic Programmer", "Hunt & Thomas — 20th anniversary edition, software craftsmanship", "Books", "49.99", 200, "https://images.example.com/pragmatic-programmer.jpg"),
            p("Clean Code", "Robert C. Martin — handbook of agile software craftsmanship", "Books", "39.99", 175, "https://images.example.com/clean-code.jpg"),
            p("Designing Data-Intensive Applications", "Martin Kleppmann — reliable, scalable, maintainable systems", "Books", "54.99", 150, "https://images.example.com/ddia.jpg"),
            p("The Art of War", "Sun Tzu — ancient Chinese military treatise on strategy", "Books", "9.99", 400, "https://images.example.com/art-of-war.jpg"),
            p("Sapiens: A Brief History of Humankind", "Yuval Noah Harari — 70,000 years of human history", "Books", "17.99", 350, "https://images.example.com/sapiens.jpg"),
            p("Dune", "Frank Herbert — epic science fiction, political and religious intrigue", "Books", "18.99", 300, "https://images.example.com/dune.jpg"),
            p("The Great Gatsby", "F. Scott Fitzgerald — the Jazz Age and the American Dream", "Books", "12.99", 450, "https://images.example.com/gatsby.jpg"),
            p("System Design Interview Vol. 2", "Alex Xu — advanced distributed systems for tech interviews", "Books", "44.99", 220, "https://images.example.com/system-design-vol2.jpg"),
            p("Deep Work", "Cal Newport — focused success in a distracted world", "Books", "15.99", 280, "https://images.example.com/deep-work.jpg"),
            p("The Lean Startup", "Eric Ries — build-measure-learn, validated learning in startups", "Books", "14.99", 320, "https://images.example.com/lean-startup.jpg"),
            p("1984", "George Orwell — totalitarianism, surveillance, and doublethink", "Books", "11.99", 500, "https://images.example.com/1984.jpg"),
            p("Domain-Driven Design", "Eric Evans — tackling complexity in software development", "Books", "59.99", 130, "https://images.example.com/ddd-evans.jpg"),
            p("The Psychology of Money", "Morgan Housel — timeless lessons on wealth, greed, and happiness", "Books", "18.99", 380, "https://images.example.com/psychology-of-money.jpg"),
            p("Spring Boot in Action", "Craig Walls — modern Spring Boot application development guide", "Books", "49.99", 160, "https://images.example.com/spring-boot-action.jpg"),

            // ── Sports & Fitness (15) ────────────────────────────────────────
            p("Peloton Bike+", "HD rotating touchscreen, Auto-Follow resistance, rear-facing speakers", "Sports", "2495.00", 8, "https://images.example.com/peloton-bike-plus.jpg"),
            p("TRX Home2 System", "Suspension trainer, full-body workout, door anchor, mesh bag", "Sports", "199.95", 70, "https://images.example.com/trx-home2.jpg"),
            p("Garmin Fenix 7X Pro", "Multisport GPS watch, solar charging, sapphire crystal, maps", "Sports", "899.99", 25, "https://images.example.com/garmin-fenix-7x.jpg"),
            p("Wilson Pro Staff 97 Tennis Racket", "97 sq in, 315 g, Braided Graphite, X2-Ergo handle", "Sports", "229.99", 40, "https://images.example.com/wilson-prostaff.jpg"),
            p("Bowflex SelectTech 552 Dumbbells", "Adjustable 5–52.5 lb, 15 weight settings, 2-year warranty", "Sports", "429.00", 30, "https://images.example.com/bowflex-552.jpg"),
            p("Hydro Flask 32 oz Water Bottle", "TempShield insulation, 18/8 stainless steel, leak-proof lid", "Sports", "44.95", 400, "https://images.example.com/hydroflask-32.jpg"),
            p("Manduka PRO Yoga Mat", "6mm thick, slip-resistant, lifetime guarantee, eco-certified rubber", "Sports", "120.00", 90, "https://images.example.com/manduka-pro.jpg"),
            p("Fitbit Charge 6", "Heart rate, GPS, stress management score, SpO2, 7-day battery", "Sports", "159.95", 120, "https://images.example.com/fitbit-charge-6.jpg"),
            p("Osprey Atmos AG 65 Backpack", "Anti-gravity suspension, hip belt pockets, rain cover included", "Sports", "290.00", 35, "https://images.example.com/osprey-atmos-65.jpg"),
            p("Shimano Deore XT Groupset", "12-speed MTB drivetrain, 10-51T cassette, hydraulic brakes", "Sports", "549.99", 20, "https://images.example.com/shimano-xt.jpg"),
            p("Callaway Rogue ST Max Driver", "Jailbreak AI Speed Frame, Triaxial Carbon crown, 10.5° loft", "Sports", "399.99", 28, "https://images.example.com/callaway-rogue.jpg"),
            p("Speedo Fastskin Pure Focus Goggles", "Low profile, ultra seal, anti-fog, UV protection, competition", "Sports", "34.99", 200, "https://images.example.com/speedo-goggles.jpg"),
            p("Black Diamond Momentum Harness", "Streamlined waistbelt, dual-core construction, 4 gear loops", "Sports", "59.95", 60, "https://images.example.com/bd-momentum.jpg"),
            p("Titleist Pro V1 Golf Balls (Dozen)", "Tour-proven, 352 tetrahedral dimples, high trajectory", "Sports", "54.99", 150, "https://images.example.com/titleist-prov1.jpg"),
            p("CAP Barbell Olympic Weight Set 110 lb", "7-foot bar, chrome sleeves, tri-grip plates, 2\" diameter", "Sports", "219.99", 15, "https://images.example.com/cap-olympicset.jpg"),

            // ── Kitchen & Dining (15) ────────────────────────────────────────
            p("Instant Pot Duo 7-in-1 8 Qt", "Pressure cooker, slow cooker, rice cooker, steamer, sauté, yogurt maker", "Kitchen", "99.95", 110, "https://images.example.com/instant-pot-duo8.jpg"),
            p("Vitamix A3500 Ascent Blender", "Self-detect technology, wireless connectivity, 2.2 HP motor", "Kitchen", "649.95", 25, "https://images.example.com/vitamix-a3500.jpg"),
            p("KitchenAid Stand Mixer 5 Qt", "10 attachments, tilt-head, 59-point planetary mixing action", "Kitchen", "449.99", 35, "https://images.example.com/kitchenaid-mixer.jpg"),
            p("Cuisinart 14-Cup Food Processor", "4mm slicing disc, shredding disc, stainless chopping blade", "Kitchen", "199.95", 45, "https://images.example.com/cuisinart-fp14.jpg"),
            p("Lodge 12\" Cast Iron Skillet", "Pre-seasoned, oven-safe to 500°F, compatible with all cooktops", "Kitchen", "34.90", 300, "https://images.example.com/lodge-skillet.jpg"),
            p("Breville Barista Express Espresso Machine", "Built-in grinder, 15 bar pressure, steam wand, 67 oz tank", "Kitchen", "699.95", 18, "https://images.example.com/breville-barista.jpg"),
            p("All-Clad D3 10-Piece Cookware Set", "3-ply stainless steel, oven-safe to 600°F, dishwasher-safe", "Kitchen", "699.99", 12, "https://images.example.com/allclad-d3.jpg"),
            p("OXO Good Grips 11-Piece Tool Set", "BPA-free, silicone heads, dishwasher-safe, rotating crock", "Kitchen", "89.99", 80, "https://images.example.com/oxo-toolset.jpg"),
            p("Ninja Foodi 6-in-1 Air Fryer", "6 qt, 8 cooking functions, DualZone technology, 1760 W", "Kitchen", "129.99", 65, "https://images.example.com/ninja-foodi-airfryer.jpg"),
            p("Pyrex 18-Piece Glass Storage Set", "BPA-free lids, microwave/dishwasher/freezer safe, nesting design", "Kitchen", "44.99", 200, "https://images.example.com/pyrex-18pc.jpg"),
            p("Global G-2 8\" Chef's Knife", "CROMOVA 18 stainless, seamless construction, dimpled blade", "Kitchen", "109.95", 55, "https://images.example.com/global-g2.jpg"),
            p("Cuisinart Cordless Electric Kettle", "1.7 L, 1500 W, BPA-free, 60-min keep warm, stainless", "Kitchen", "59.99", 90, "https://images.example.com/cuisinart-kettle.jpg"),
            p("Bamboo Cutting Board Set of 3", "Organic bamboo, juice groove, non-slip feet, antimicrobial", "Kitchen", "32.99", 250, "https://images.example.com/bamboo-board-3pc.jpg"),
            p("Zojirushi Rice Cooker & Warmer 5.5 Cup", "Fuzzy logic technology, retractable cord, keep-warm mode", "Kitchen", "149.99", 40, "https://images.example.com/zojirushi-rice.jpg"),
            p("Bialetti Moka Express 6-Cup", "Aluminum stovetop espresso maker, iconic octagonal design", "Kitchen", "34.99", 180, "https://images.example.com/bialetti-moka.jpg"),

            // ── Home & Garden (10) ────────────────────────────────────────────
            p("Dyson V15 Detect Cordless Vacuum", "Laser dust detection, HEPA filtration, 60-min runtime, LCD screen", "Home", "749.99", 22, "https://images.example.com/dyson-v15.jpg"),
            p("Philips Hue Starter Kit (4 Bulbs)", "Color ambiance, A19, 800 lm, voice & app control, Zigbee hub", "Home", "199.99", 75, "https://images.example.com/philips-hue-kit.jpg"),
            p("Nest Learning Thermostat 3rd Gen", "Auto-schedule, energy history, remote control, stainless steel", "Home", "249.99", 50, "https://images.example.com/nest-thermostat.jpg"),
            p("IKEA KALLAX 4x4 Shelf Unit", "White, 30\" x 57.7\", cube storage, compatible with inserts", "Home", "129.00", 30, "https://images.example.com/ikea-kallax.jpg"),
            p("Coway AP-1512HH Air Purifier", "4-stage filtration, HEPA, coverage 360 sq ft, eco mode", "Home", "109.99", 60, "https://images.example.com/coway-ap1512.jpg"),
            p("WD 4TB Elements External Hard Drive", "USB 3.0, plug-and-play, works with PC & Mac, formatted NTFS", "Home", "89.99", 140, "https://images.example.com/wd-elements-4tb.jpg"),
            p("Fiskars 46\" Steel D-Handle Garden Spade", "Hardened steel blade, rust-resistant, cushion grip D-handle", "Home", "44.99", 85, "https://images.example.com/fiskars-spade.jpg"),
            p("Ring Video Doorbell Pro 2", "Head-to-toe HD video, 3D motion detection, dual-band Wi-Fi", "Home", "249.99", 40, "https://images.example.com/ring-pro-2.jpg"),
            p("LEVOIT Core 300 Air Purifier", "True HEPA, covers 219 sq ft, 24 dB quiet, night-light mode", "Home", "99.99", 95, "https://images.example.com/levoit-core300.jpg"),
            p("Miracle-Gro Potting Mix 2 cu ft", "Feeds plants for up to 6 months, prevents overwatering", "Home", "14.88", 400, "https://images.example.com/miracle-gro-potting.jpg"),

            // ── Beauty & Health (10) ──────────────────────────────────────────
            p("Cetaphil Gentle Skin Cleanser 20 oz", "Soap-free, non-foaming, dermatologist recommended for sensitive skin", "Beauty", "15.99", 400, "https://images.example.com/cetaphil-cleanser.jpg"),
            p("CeraVe Moisturizing Cream 19 oz", "Hyaluronic acid, ceramides, MVE delivery technology, fragrance-free", "Beauty", "19.99", 350, "https://images.example.com/cerave-cream.jpg"),
            p("Neutrogena Hydro Boost Water Gel", "Hyaluronic acid, oil-free, non-comedogenic, dermatologist tested", "Beauty", "21.99", 300, "https://images.example.com/neutrogena-hydroboost.jpg"),
            p("EltaMD UV Clear SPF 46 Sunscreen", "Broad-spectrum, zinc oxide 9%, niacinamide, oil-free 1.7 oz", "Beauty", "40.00", 200, "https://images.example.com/eltamd-uvclear.jpg"),
            p("Revlon One-Step Hair Dryer & Volumizer", "Oval brush, ceramic ionic technology, 2 heat/2 speed settings", "Beauty", "59.99", 130, "https://images.example.com/revlon-one-step.jpg"),
            p("Oral-B Pro 1000 Electric Toothbrush", "CrossAction brush head, pressure sensor, 2-minute timer, rechargeable", "Beauty", "49.99", 180, "https://images.example.com/oralb-pro1000.jpg"),
            p("Kiehl's Ultra Facial Cream 1.7 oz", "24-hour hydration, squalane, Imperata cylindrica, all skin types", "Beauty", "42.00", 150, "https://images.example.com/kiehls-ufc.jpg"),
            p("Theragun Mini 2nd Gen Percussive Massager", "3 attachments, 150-min battery, QuietForce technology, 3 speeds", "Beauty", "199.00", 60, "https://images.example.com/theragun-mini.jpg"),
            p("Differin Adapalene Gel 0.1% Acne Treatment", "Retinoid-like prescription-strength, 45 g, once-daily use", "Beauty", "14.88", 350, "https://images.example.com/differin-gel.jpg"),
            p("Vitamix TurboBlend 4500 Personal Blender", "32 oz container, self-cleaning, aircraft-grade stainless blades", "Beauty", "99.99", 70, "https://images.example.com/vitamix-personal.jpg")
        );
    }
}
