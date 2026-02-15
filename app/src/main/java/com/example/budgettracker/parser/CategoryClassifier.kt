package com.example.budgettracker.parser

object CategoryClassifier {

    private val categoryRules = mapOf(
        "Food" to listOf("swiggy", "zomato", "dominos", "mcdonald", "restaurant", "cafe"),
        "Transport" to listOf("uber", "ola", "rapido", "metro", "fuel", "petrol"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "ajio"),
        "Entertainment" to listOf("netflix", "spotify", "hotstar", "prime video"),
        "Bills" to listOf("electricity", "water", "bill", "recharge", "airtel", "jio"),
        "Cash" to listOf("atm", "cash withdrawal")
    )

    fun detectCategory(merchant: String?): String {
        if (merchant.isNullOrBlank()) return "General"

        val lower = merchant.lowercase()

        categoryRules.forEach { (category, keywords) ->
            if (keywords.any { lower.contains(it) }) {
                return category
            }
        }

        return "General"
    }
}
