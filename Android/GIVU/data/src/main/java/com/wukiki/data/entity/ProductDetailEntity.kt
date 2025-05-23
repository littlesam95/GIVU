package com.wukiki.data.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProductEntity(
    @Json(name = "product")
    val product: ProductDetailEntity,

    @Json(name = "reviews")
    val reviews: List<ProductReviewEntity>,

    @Json(name = "likeCount")
    val likeCount: Int,

    @Json(name = "likedByUser")
    val isLiked: Boolean
)

@JsonClass(generateAdapter = true)
data class ProductDetailEntity(
    @Json(name = "id")
    val id: Int,

    @Json(name = "productName")
    val productName: String,

    @Json(name = "category")
    val category: String,

    @Json(name = "price")
    val price: Int,

    @Json(name = "image")
    val image: String,

    @Json(name = "star")
    val star: Double,

    @Json(name = "createdAt")
    val createdAt: String,

    @Json(name = "description")
    val description: String
)