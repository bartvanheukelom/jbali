package org.jbali.spring

import org.jbali.util.OneTimeFlag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import javax.annotation.PreDestroy
import kotlin.test.*

private val log = LoggerFactory.getLogger(SpringCodeContextTest::class.java)

class SpringCodeContextTest {
    
//    @Test fun testWithConfig() {
//        AnnotationConfigApplicationContext(PlumbingCompany::class.java).use { ctx ->
//            log.info("Beans:\n${ctx.singletonBeanTable()}")
//            ctx.getBean<Drinker>()
//            log.info("End of use")
//        }
//    }
    
    @Test fun testNoConfig() {
    
        lateinit var soup: Soup
        AnnotationConfigApplicationContext().apply {
    
            addApplicationListener { e -> log.info("ApplicationEvent: $e") }
            
            with(beanFactory) {
                // required to make @PreDestroy and similar annotations work
                // in GenericApplicationContext, but not in AnnotationConfigApplicationContext
//                addBeanPostProcessor(CommonAnnotationBeanPostProcessor())
    
                log.info("Going to register singletons")
                registerSingleton("onion", Onion)
            }
            
            // order doesn't matter
            log.info("Going to register beans")
            registerBean<WaterTapper>()
            registerBean<Soup>()
            registerBean { MeatFactory(MeatFactory.password) }
            registerBean<Supervisor>()
            registerBean<Toolbox>()
            registerBean { Plumber() }
            registerBean<Drinker>()
    
            log.info("Going to refresh")
            refresh()
        }.use { ctx ->
            log.info("Beans:\n${ctx.singletonBeanTable()}")
            
            // even though the Onion is an external singleton, the following works
            ctx.getBean<Vegetable>()
            
            soup = ctx.getBean()
            
            assertFails {
                // wasn't registered
                ctx.getBean<Sauce>()
            }.let { log.info("Sauce: $it") }
            
            log.info("End of use")
        }
        log.info("Context closed")
    
        assertTrue(soup.destroyed, "soup not destroyed")
        assertFalse(Water.destroyed, "water was destroyed")
        // Onion should not be destroyed, it will throw if that is attempted (TODO but that is swallowed by spring)
        
    }
    
}


// --- external singleton --- //

interface Vegetable

object Onion : Vegetable {
    @PreDestroy fun destroy() {
        log.info("Onion: destroy()")
        fail("object is indestructible")
    }
}



// --- FactoryBean --- //

interface MysteryIngredient

class Meat : MysteryIngredient {
    init {
        log.info("Meat: init()")
    }
    @PreDestroy fun destroy() {
        log.info("Meat: destroy()")
    }
}

class Supervisor {
    init {
        log.info("Supervisor: init()")
    }
    @PreDestroy fun destroy() {
        log.info("Supervisor: destroy()")
    }
}

class MeatFactory(
    // ensure that this factory cannot be autowired
    password: String
) : FactoryBean<Meat>, BeanFactoryAware {
    
    companion object {
        const val password = "letsMakeSomeMeat"
    }
    
    init {
        log.info("MeatFactory: init()")
        require(password == Companion.password)
    }
    @PreDestroy fun destroy() {
        log.info("MeatFactory: destroy()")
    }
    
    private lateinit var bf: BeanFactory
    
    override fun setBeanFactory(beanFactory: BeanFactory) {
        bf = beanFactory
    }
    
    override fun getObjectType(): Class<*> = Meat::class.java
    override fun getObject(): Meat {
        log.info("MeatFactory: asked to create meat, need supervisor first")
        bf.getBean<Supervisor>()
        log.info("MeatFactory: going to create meat")
        return Meat()
    }
}


// --- bean that produces another bean --- //

class Water {
    companion object {
        var destroyed = false
        var count = 0
    }
    private val i = ++count
    init {
        log.info("Water: init($i)")
    }
    @PreDestroy fun destroy() {
        log.info("Water: destroy($i)")
        // throwing here would just be swallowed by spring
        destroyed = true
    }
    
    override fun toString() = "Water($i)"
}

class Tap(
    wrench: Wrench
) {
    init {
        log.info("Tap: init()")
    }
    @PreDestroy fun destroy() {
        log.info("Tap: destroy()")
    }
    
    // TODO these annotations have no effect, why? see WaterTapper.
    @Bean
    @Scope("prototype")
    fun tapWater() = Water()
}

class Wrench

class Toolbox {
    val wrench = Wrench()
}

class Plumber {
    init {
        log.info("Plumber: init()")
    }
    @PreDestroy fun destroy() {
        log.info("Plumber: destroy()")
    }
    
    @Bean fun installTap(toolbox: Toolbox) = Tap(toolbox.wrench)
}

// TODO why is this required, even though Tap itself exposes @Bean tapWater()?
class WaterTapper(private val tap: Tap) {
    @Bean
    @Scope("prototype")
    fun water() = tap.tapWater()
}

class Drinker(water: Water) {
    init {
        log.info("Drinker: init(water=$water)")
    }
}

@Configuration
open class PlumbingCompany {
    @Bean open fun plumber() = Plumber()
    @Bean open fun drinker(tap: Tap) = Drinker(tap.tapWater())
}

// --- the one that depends on it all --- //

class Soup(
    water: Water,
    vegetable: Vegetable,
    mystery: MysteryIngredient,
) {
    
    init {
        log.info("Soup: init(water=$water, vegetable=$vegetable, mystery=$mystery)")
    }
    
    var destroyed by OneTimeFlag()
    @PreDestroy fun destroy() {
        log.info("Soup: destroy()")
        destroyed = true
    }
}

// never constructed
class Sauce
