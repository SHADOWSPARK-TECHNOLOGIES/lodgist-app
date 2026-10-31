package com.example.data.remote.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT

/** Reads the route string out of a Retrofit verb annotation, whatever shape it has. */
private fun pathOf(annotation: Annotation?): String? {
    if (annotation == null) return null
    val member = annotation.annotationClass.java.getMethod("value").invoke(annotation)
    return when (member) {
        is Array<*> -> member.firstOrNull() as? String
        is String -> member
        else -> null
    }
}

/**
 * Guards the boundary between this client and the Lodgist API.
 *
 * The prototype once declared five `api/v1/properties*` routes that the
 * backend never served, so every screen built on them could only 404. This
 * test fails the build if an endpoint is declared that the API does not have.
 */
class LodgistApiServiceContractTest {

    private fun declaredRoutes(): Set<String> {
        val routes = mutableSetOf<String>()
        for (method in LodgistApiService::class.java.declaredMethods) {
            val path = pathOf(method.getAnnotation(GET::class.java))
                ?: pathOf(method.getAnnotation(POST::class.java))
                ?: pathOf(method.getAnnotation(PUT::class.java))
                ?: pathOf(method.getAnnotation(PATCH::class.java))
                ?: pathOf(method.getAnnotation(DELETE::class.java))
            if (path != null) routes.add(path)
        }
        return routes
    }

    @Test
    fun `client declares exactly the routes the api serves`() {
        assertEquals(setOf("api/listings", "api/listings/{id}"), declaredRoutes())
    }

    @Test
    fun `no client declaration targets an unimplemented versioned namespace`() {
        val versioned = declaredRoutes().filter { route -> route.contains("/v1/") }
        assertTrue(
            "These routes do not exist on the Lodgist API: $versioned",
            versioned.isEmpty()
        )
    }
}
