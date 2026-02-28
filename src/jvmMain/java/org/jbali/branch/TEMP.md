# Branching Continuation System - Analysis

## Overview

This is an experimental system for executing Kotlin suspend functions with multiple execution paths. It allows code to "branch" at decision points and explore all possible paths, returning a map of all execution paths and their results.

## Core API

```kotlin
interface Branching {
    suspend fun <B> branch(name: String? = null, options: List<B>): B
}

fun <T> runBranching(block: suspend Branching.() -> T): Map<String, Result<T>>
```

Example:
```kotlin
runBranching {
    when (branch("animal", listOf(Dog, Cat))) {
        Dog -> "Woof"
        Cat -> when (branch("mood", listOf(true, false))) {
            true -> "Meow"
            false -> error("Leave me alone")
        }
    }
}
// Returns:
// {
//   "animal=Dog" -> Success("Woof"),
//   "animal=Cat/mood=true" -> Success("Meow"),
//   "animal=Cat/mood=false" -> Failure("Leave me alone")
// }
```

## Two Implementation Strategies

### Strategy P - CAPTURING (capturing.kt)

**Approach**: Capture-and-resume recursively

1. Suspend function hits a branch point
2. `suspendCoroutine` captures the continuation and returns it to the outer coordinator
3. Coordinator clones the continuation N times (one per option)
4. Coordinator calls `invokeSuspend(value)` on each clone with each option value
5. If a clone suspends again (another branch), recursively handle it
6. If a clone completes, store the result in the map

**Flow**:
```
block.invoke()
  -> hits branch()
  -> suspendCoroutine { capturedCont }
  -> return to coordinator with BranchPoint(options, capturedCont)
coordinator:
  for each option:
    clone = capturedCont.clone()
    result = clone.invokeSuspend(option)
    if result == SUSPENDED:
      recursively handle next branch
    else:
      store result
```

**Limitations**:
- Only works if all branches happen in the same continuation class
- Fails when branching spans multiple suspend function calls (e.g., `testMemberFun`)
- The recursive call assumes the clone suspended in the same method (comment at capturing.kt:111)

### Strategy B - CALLBACK (callback.kt)

**Approach**: Handle all branching inline at each suspension point

1. Suspend function hits a branch point
2. Inside the `suspendCoroutine` callback, immediately handle all options
3. Clone the continuation N-1 times (reuse original for last option)
4. Immediately call `invokeSuspend(value)` on each clone within the same callback
5. Each clone runs independently and may hit further branches
6. Results are stored directly in the shared map as they complete

**Flow**:
```
block.invoke()
  -> hits branch()
  -> suspendCoroutine { cont }
    inside callback:
      for each option:
        clone = cont.clone() (except last)
        result = clone.invokeSuspend(option)
        if result == SUSPENDED:
          // will hit another branch() which handles itself
        else:
          store result
```

**Advantages**:
- All branching is self-contained at each suspension point
- No coordination needed outside the branch() function
- Should work across multiple suspend function calls

## Technical Implementation Details

### Continuation Cloning (continuations.kt)

```kotlin
fun clone(cont: Continuation<*>): Continuation<*> {
    val clone = theUnsafe.allocateInstance(type) as Continuation<Any?>
    fields.forEach { it.set(clone, it.get(cont)) }
    return clone
}
```

- Uses `sun.misc.Unsafe.allocateInstance()` to create object without calling constructor
- Reflectively copies all fields from original to clone
- This creates a perfect snapshot of the continuation's state at that moment
- Each clone can then be resumed with different values to explore different paths

### SafeContinuation Unwrapping (continuations.kt:8-17)

Kotlin's `suspendCoroutine` wraps the actual continuation in a `SafeContinuation` for thread-safety. We need the underlying delegate to clone it:

```kotlin
val f_delegate = SafeContinuation.class.declaredField("delegate")
val actualCont = f_delegate.get(safeCont)
```

### Manual Coroutine Invocation

Both strategies manually invoke the continuation's `invokeSuspend` method:

```kotlin
val result = continuation.invokeSuspend(value)
when (result) {
    COROUTINE_SUSPENDED -> // hit another suspension point
    else -> // completed with result
}
```

This bypasses Kotlin's normal coroutine machinery to directly control execution.

## Test Results (from MultiResumeTest.kt comments)

| Test | Strategy P (CAPTURING) | Strategy B (CALLBACK) |
|------|------------------------|----------------------|
| `testLambda` | ✅ Pass | ❓ Unknown |
| `testResumeForward` | ✅ Pass | ❓ Unknown |
| `testExample` | ✅ Pass | ❓ Unknown |
| `testMemberFun` | ❌ FAIL | ❓ Unknown |

### testMemberFun Failure

This test branches within `voice()`, which then calls `boolz()` which also branches:

```kotlin
suspend fun voice(): String {
    val animal = branch(listOf(Dog, Cat))
    return when (animal) {
        Cat -> when (boolz()) { ... }
        ...
    }
}

suspend fun boolz(): Boolean {
    val b = branch(listOf(1, 0, 42))
    return b == 1
}
```

**Why CAPTURING fails**: When `boolz()` suspends and returns a new continuation class, the recursive `doBranch(clone)` call in capturing.kt:111 tries to resume the `voice()` continuation, not the `boolz()` continuation. The comment acknowledges this: `// TODO only works if we suspended in the same method`

**Why CALLBACK should work**: Each branch handles itself independently, so when `boolz()` hits its branch, it will handle all its options right there, regardless of who called it.

## Current Status

- `runBranching()` currently defaults to `CAPTURING` strategy (run.kt:5)
- `CALLBACK` is commented out (run.kt:6)
- Need to make strategy selectable and test both

## Implementation Tasks

1. Create `BranchingStrategy` enum
2. Add strategy parameter to `runBranching()`
3. Add comprehensive KDoc everywhere
4. Parameterize tests to run with both strategies
