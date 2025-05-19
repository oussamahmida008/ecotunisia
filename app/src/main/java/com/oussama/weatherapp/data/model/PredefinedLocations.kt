package com.oussama.weatherapp.data.model

import java.util.Date

/**
 * Predefined environmental locations in Tunisia
 */
object PredefinedLocations {
    
    /**
     * Get a list of predefined environmental locations in Tunisia
     */
    fun getTunisianEcoLocations(userId: String, userName: String): List<Location> {
        return listOf(
            Location(
                id = "ichkeul_national_park",
                title = "Ichkeul National Park",
                description = "A UNESCO World Heritage site and important wetland ecosystem. Home to migratory birds and diverse plant species. The park includes Lake Ichkeul, marshes, and a mountain.",
                latitude = 37.1402,
                longitude = 9.6778,
                userId = userId,
                userName = userName,
                timestamp = Date(),
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3f/Ichkeul_Lake.jpg/1200px-Ichkeul_Lake.jpg"
            ),
            Location(
                id = "bou_hedma_national_park",
                title = "Bou Hedma National Park",
                description = "A UNESCO Biosphere Reserve known for its acacia forests and wildlife conservation efforts. Home to reintroduced species like the Scimitar-horned Oryx and Addax.",
                latitude = 34.4947,
                longitude = 9.6481,
                userId = userId,
                userName = userName,
                timestamp = Date(),
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d9/Parc_national_de_Bouhedma_01.jpg/1200px-Parc_national_de_Bouhedma_01.jpg"
            ),
            Location(
                id = "zembra_island",
                title = "Zembra Island Marine Reserve",
                description = "A protected marine area and national park with rich biodiversity. Important for seabird conservation and Mediterranean marine ecosystems.",
                latitude = 37.1167,
                longitude = 10.8000,
                userId = userId,
                userName = userName,
                timestamp = Date(),
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b9/Zembra_Island.jpg/1200px-Zembra_Island.jpg"
            ),
            Location(
                id = "el_feidja_national_park",
                title = "El Feidja National Park",
                description = "A forested mountain area with oak and pine forests. Important for wildlife conservation, including the endangered Barbary deer.",
                latitude = 36.5000,
                longitude = 8.3333,
                userId = userId,
                userName = userName,
                timestamp = Date(),
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/El_Feija_National_Park.jpg/1200px-El_Feija_National_Park.jpg"
            ),
            Location(
                id = "chaambi_national_park",
                title = "Chaambi National Park",
                description = "Tunisia's highest mountain and a diverse ecosystem with various plant and animal species. Important for biodiversity conservation.",
                latitude = 35.2167,
                longitude = 8.6833,
                userId = userId,
                userName = userName,
                timestamp = Date(),
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9f/Djebel_Chambi.jpg/1200px-Djebel_Chambi.jpg"
            ),
            Location(
                id = "sidi_bou_said_eco_garden",
                title = "Sidi Bou Said Eco Garden",
                description = "A beautiful eco-friendly garden in the picturesque village of Sidi Bou Said. Features Mediterranean plants and sustainable gardening practices.",
                latitude = 36.8711,
                longitude = 10.3417,
                userId = userId,
                userName = userName,
                timestamp = Date()
            ),
            Location(
                id = "cap_bon_biosphere",
                title = "Cap Bon Biosphere Reserve",
                description = "A diverse peninsula with important coastal ecosystems, wetlands, and forests. Home to various bird species and Mediterranean flora.",
                latitude = 36.8833,
                longitude = 11.1167,
                userId = userId,
                userName = userName,
                timestamp = Date()
            ),
            Location(
                id = "kuriat_islands",
                title = "Kuriat Islands Marine Protected Area",
                description = "Important sea turtle nesting site and marine conservation area. Efforts focus on protecting loggerhead turtles and marine biodiversity.",
                latitude = 35.7667,
                longitude = 11.0167,
                userId = userId,
                userName = userName,
                timestamp = Date(),
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a0/Kuriat_Islands.jpg/1200px-Kuriat_Islands.jpg"
            ),
            Location(
                id = "zaghouan_water_temple",
                title = "Zaghouan Water Temple & Eco Park",
                description = "Historic Roman water temple and surrounding ecological park. Features water conservation education and native plant restoration.",
                latitude = 36.4028,
                longitude = 10.1428,
                userId = userId,
                userName = userName,
                timestamp = Date()
            ),
            Location(
                id = "djerba_eco_farm",
                title = "Djerba Organic Farm",
                description = "Sustainable agriculture project on Djerba island showcasing traditional farming methods and water conservation techniques.",
                latitude = 33.8000,
                longitude = 10.8500,
                userId = userId,
                userName = userName,
                timestamp = Date()
            )
        )
    }
}
