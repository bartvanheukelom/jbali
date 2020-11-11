package org.jbali.hum

import kotlinx.serialization.Serializable

@Serializable(with = Animalia.Companion::class)
sealed class Animalia(
        val simpleName: String
) : HumValue<Animalia>(Companion) {

    companion object : HumRoot<Animalia>(Animalia::class)

    sealed class Carnivora(simpleName: String) : Animalia(simpleName) {

        companion object : HumBranch<Animalia, Carnivora>(Animalia::class, Carnivora::class)

        sealed class Felidae(simpleName: String) : Carnivora(simpleName) {

            companion object : HumBranch<Animalia, Felidae>(Animalia::class, Felidae::class)

            object FCatus : Felidae("domestic cat")
            object PPardus : Felidae("leopard")
            object PLeo : Felidae("lion")

        }

        sealed class Caniformia(simpleName: String) : Carnivora(simpleName) {

            companion object : HumBranch<Animalia, Caniformia>(Animalia::class, Caniformia::class)

            object CanisLupus : Carnivora("wolf")
            object UrsusArctos : Carnivora("brown bear")

        }

        object NandiniaBinotata : Carnivora("african palm civet")

    }

    sealed class Rodentia(simpleName: String) : Animalia(simpleName) {

        companion object : HumBranch<Animalia, Rodentia>(Animalia::class, Rodentia::class)

        object MusMusculus : Rodentia("house mouse")

    }

}
