package com.example.checkmate

data class User(var username: String ?= null, var email: String ?= null, var password: String ?= null,
                var venmo: String ?= null)