package com.example.checkmate

data class Receipt(var title: String, var date: String, var payer: String, var priceList: List<String>,
                   var itemList: List<String>, var payerList: List<String>)