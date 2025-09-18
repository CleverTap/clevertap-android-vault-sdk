/**
 * Interface for converting between generic types and strings
 */
interface TypeConverter<T> {
    fun toString(value: T): String
    fun fromString(value: String): T
    fun fromStringNullable(value: String?): T?
    val typeName: String
}

/**
 * Registry of type converters for supported data types
 */
object TypeConverterRegistry {
    private val converters = mutableMapOf<Class<*>, TypeConverter<*>>()

    init {
        // Register built-in converters
        register(String::class.java, StringConverter())
        register(Int::class.java, IntConverter())
        register(Long::class.java, LongConverter())
        register(Float::class.java, FloatConverter())
        register(Double::class.java, DoubleConverter())
        register(Boolean::class.java, BooleanConverter())
    }

    fun <T> register(clazz: Class<T>, converter: TypeConverter<T>) {
        converters[clazz] = converter
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getConverter(clazz: Class<T>): TypeConverter<T>? {
        return converters[clazz] as? TypeConverter<T>
    }
}

// ============================================
// BUILT-IN TYPE CONVERTERS
// ============================================

class StringConverter : TypeConverter<String> {
    override fun toString(value: String): String = value
    override fun fromString(value: String): String = value
    override fun fromStringNullable(value: String?): String? = value
    override val typeName: String = "string"
}

class IntConverter : TypeConverter<Int> {
    override fun toString(value: Int): String = value.toString()
    override fun fromString(value: String): Int =
        value.toIntOrNull() ?: throw NumberFormatException("Cannot convert '$value' to Int")
    override fun fromStringNullable(value: String?): Int? = value?.toIntOrNull()
    override val typeName: String = "integer"
}

class LongConverter : TypeConverter<Long> {
    override fun toString(value: Long): String = value.toString()
    override fun fromString(value: String): Long =
        value.toLongOrNull() ?: throw NumberFormatException("Cannot convert '$value' to Long")
    override fun fromStringNullable(value: String?): Long? = value?.toLongOrNull()
    override val typeName: String = "long"
}

class FloatConverter : TypeConverter<Float> {
    override fun toString(value: Float): String = value.toString()
    override fun fromString(value: String): Float =
        value.toFloatOrNull() ?: throw NumberFormatException("Cannot convert '$value' to Float")
    override fun fromStringNullable(value: String?): Float? = value?.toFloatOrNull()
    override val typeName: String = "float"
}

class DoubleConverter : TypeConverter<Double> {
    override fun toString(value: Double): String = value.toString()
    override fun fromString(value: String): Double =
        value.toDoubleOrNull() ?: throw NumberFormatException("Cannot convert '$value' to Double")
    override fun fromStringNullable(value: String?): Double? = value?.toDoubleOrNull()
    override val typeName: String = "double"
}

class BooleanConverter : TypeConverter<Boolean> {
    override fun toString(value: Boolean): String = value.toString()
    override fun fromString(value: String): Boolean = when (value.lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw IllegalArgumentException("Cannot convert '$value' to Boolean")
    }
    override fun fromStringNullable(value: String?): Boolean? = when (value?.lowercase()) {
        "true" -> true
        "false" -> false
        null -> null
        else -> throw IllegalArgumentException("Cannot convert '$value' to Boolean")
    }
    override val typeName: String = "boolean"
}