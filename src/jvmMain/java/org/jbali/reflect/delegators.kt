package org.jbali.reflect

/**
 * Create a proxy which delegates all calls to the result of the given delegate supplier,
 * which is invoked for each and every call.
 */
inline fun <reified I : Any> createDelegator(
        classLoader: ClassLoader? = null,
        noinline name: () -> String = { "" },
        noinline delegateSupplier: () -> I
): I =
        createProxy(
                classLoader = classLoader
        ) { mi ->
            delegateSupplier().let { d ->
                if (mi.invokesToString) {
                    "DelegatorProxy[${name()} -> $d]"
                } else {
                    // TODO d.invokeTransparent(mi)
                    Proxies.invokeTransparent(mi.method, d, mi.args.toTypedArray())
                }
            }
        }
