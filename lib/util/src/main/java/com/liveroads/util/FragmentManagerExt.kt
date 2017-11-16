package com.liveroads.util

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction

/**
 * Find a fragment by its ID and throw an exception if it is not found or was found but is not an
 * instance of the expected type.
 * @param id the ID of the fragment to find.
 * @return the fragment that was found.
 * @throws FragmentNotFoundException if no fragment was found with the given ID.
 * @throws FragmentCastException if a fragment was found with the given ID but was not an instance
 * of the expected type.
 * @see FragmentManager.findFragmentById
 */
inline fun <reified T : Fragment> FragmentManager.findFragmentByIdOrThrow(id: Int): T {
    val fragment = findFragmentById(id)
    if (fragment === null) {
        throw FragmentNotFoundException("fragment with ID $id not found")
    } else if (fragment is T) {
        return fragment
    } else {
        throw FragmentCastException("fragment with ID $id was found, but was an instance "
                + "of ${fragment::class} when expecting an instance of ${T::class}")
    }
}

/**
 * Find a fragment by its tag and throw an exception if it is not found or was found but is not an
 * instance of the expected type.
 * @param tag the tag of the fragment to find.
 * @return the fragment that was found.
 * @throws FragmentNotFoundException if no fragment was found with the given tag.
 * @throws FragmentCastException if a fragment was found with the given tag but was not an instance
 * of the expected type.
 * @see FragmentManager.findFragmentByTag
 */
inline fun <reified T : Fragment> FragmentManager.findFragmentByTagOrThrow(tag: String): T {
    val fragment = findFragmentByTag(tag)
    if (fragment === null) {
        throw FragmentNotFoundException("fragment with tag $tag not found")
    } else if (fragment is T) {
        return fragment
    } else {
        throw FragmentCastException("fragment with tag $tag was found, but was an instance "
                + "of ${fragment::class} when expecting an instance of ${T::class}")
    }
}

/**
 * Find a fragment by its ID, creating and adding it if not found.
 *
 * If a fragment with the given ID is found then the given code block is not executed; otherwise,
 * the given code block is executed and the fragment that it returns is added with the given ID in
 * a newly-created [FragmentTransaction] and committed.
 *
 * @param id the ID of the fragment to find or to use when adding the fragment.
 * @param createFragment the block of code to create and return a new instance of the fragment.
 * @return the fragment that was found or created by the given code block.
 * @throws FragmentCastException if a fragment was found with the given ID but was not an instance
 * of the expected type.
 * @see FragmentManager.findFragmentById
 * @see FragmentTransaction.add
 */
inline fun <reified T : Fragment> FragmentManager.findFragmentByIdOrAdd(id: Int,
        createFragment: () -> T): T {
    val fragment = findFragmentById(id)
    if (fragment === null) {
        val newFragment = createFragment()
        beginTransaction().add(id, newFragment).commit()
        return newFragment
    } else if (fragment is T) {
        return fragment
    } else {
        throw FragmentCastException("fragment with ID $id was found, but was an instance "
                + "of ${fragment::class} when expecting an instance of ${T::class}")
    }
}

/**
 * Find a fragment by its ID, creating and replacing it if not found.
 *
 * If a fragment with the given ID is found then the given code block is not executed; otherwise,
 * the given code block is executed and the fragment that it returns is replaced with the given ID
 * in a newly-created [FragmentTransaction] and committed.
 *
 * @param id the ID of the fragment to find or to use when replacing the fragment.
 * @param createFragment the block of code to create and return a new instance of the fragment.
 * @return the fragment that was found or created by the given code block.
 * @throws FragmentCastException if a fragment was found with the given ID but was not an instance
 * of the expected type.
 * @see FragmentManager.findFragmentById
 * @see FragmentTransaction.replace
 */
inline fun <reified T : Fragment> FragmentManager.findFragmentByIdOrReplace(id: Int,
        createFragment: () -> T): T {
    val fragment = findFragmentById(id)
    if (fragment === null) {
        val newFragment = createFragment()
        beginTransaction().replace(id, newFragment).commit()
        return newFragment
    } else if (fragment is T) {
        return fragment
    } else {
        throw FragmentCastException("fragment with ID $id was found, but was an instance "
                + "of ${fragment::class} when expecting an instance of ${T::class}")
    }
}

/**
 * Find a fragment by its tag, creating and adding it if not found.
 *
 * If a fragment with the given tag is found then the given code block is not executed; otherwise,
 * the given code block is executed and the fragment that it returns is added with the given tag in
 * a newly-created [FragmentTransaction] and committed.
 *
 * @param id the tag of the fragment to find or to use when adding the fragment.
 * @param createFragment the block of code to create and return a new instance of the fragment.
 * @return the fragment that was found or created by the given code block.
 * @throws FragmentCastException if a fragment was found with the given tag but was not an instance
 * of the expected type.
 * @see FragmentManager.findFragmentById
 * @see FragmentTransaction.add
 */
inline fun <reified T : Fragment> FragmentManager.findFragmentByTagOrAdd(tag: String,
        createFragment: () -> T): T {
    val fragment = findFragmentByTag(tag)
    if (fragment === null) {
        val newFragment = createFragment()
        beginTransaction().add(newFragment, tag).commit()
        return newFragment
    } else if (fragment is T) {
        return fragment
    } else {
        throw FragmentCastException("fragment with tag $tag was found, but was an instance "
                + "of ${fragment::class} when expecting an instance of ${T::class}")
    }
}

class FragmentNotFoundException(message: String?) : RuntimeException(message)
class FragmentCastException(message: String?) : RuntimeException(message)
