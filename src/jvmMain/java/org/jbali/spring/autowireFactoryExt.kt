package org.jbali.spring

import org.springframework.beans.factory.config.AutowireCapableBeanFactory

inline fun <reified T : Any> AutowireCapableBeanFactory.createBean(): T =
    createBean(T::class.java)
