package org.jbali.spring

import org.jbali.util.cast
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * Spring condition which checks whether the bean factory already contains a bean
 * with the same name as the bean definition this condition is applied to.
 * If it does, the condition returns `false` and the bean is not created.
 *
 * Can be used, for example, to override a classpath-scanned bean with an injected singleton in a unit test.
 *
 * The definition must explicitly specify the name using the [Bean] annotation. That is, an implicit
 * bean name from e.g. the name of the factory method is not picked up.
 */
class BeanNotDefined : Condition {
    
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        
        val beanName = try {
            metadata.getAnnotationAttributes(Bean::class.qualifiedName!!)!!.getValue("name")
                .cast<Array<String>>()
                .first()
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to get name of bean annotated with @Conditional(${BeanNotDefined::class.simpleName}): $e", e)
        }
        
        return !context.beanFactory!!.containsBean(beanName)
        
    }
    
}
