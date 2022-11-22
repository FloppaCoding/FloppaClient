package floppaclient.shaders.uniforms

import floppaclient.shaders.Shader
import org.lwjgl.opengl.GL20

/**
 * Uniforms are used to communicate variables with shaders at runtime.
 * This class offers some utilities to use that functionality.
 */
abstract class Uniform<T>(shader: Shader, name: String) {

    /**
     * Stores the last value that was updated to the GPU, is used to only update the value to the GPU if it was changed.
     */
    abstract var lastValue: T?

    /**
     * Reference to the impl. Used somewhat similar to a pointer.
     */
    protected val uninformID: Int = GL20.glGetUniformLocation(shader.programID, name)

    /**
     * Updates the value of this impl to the GPU.
     * The value(s) is(are) loaded from the specified source in the respective constructors.
     */
    abstract fun update()

    //TODO generalize the check that tests whether the value should be updated here.
    // The current implementation is pretty bad, it requires the generic in the Superclass, but it is never used there.
    // Either remove the generic here and only implement it in the members or fix it to be generalized properly
    //TODO also might need some readjustments to supporter other typos of Uniform.
}