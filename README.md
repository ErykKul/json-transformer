# JSON Transformer

JSON Transformer is a simple and expressive Java library that uses transformers defined in JSON documents for transformations of JSON structures (even complex structures containing nested arrays of objects) into other (complex) JSON structures. At its bases, it uses the [JavaScript Object Notation (JSON) Pointers](https://datatracker.ietf.org/doc/html/rfc6901) specification, as provided by the `jakarta.json` API. This library extends that specification with the `[i]` notation for working with arrays. It also provides several built-in functions, ranging from the very basic `copy` function taking two JSON pointers as arguments, to more expressive `map`, `filter` and `reduce` functions taking JavaScript expressions as arguments (e.g., `res = res + x`). Together with the `script` function, that can be used to produce any JSON object (e.g., `res = { a: 1, b: myFunc(x) }`), and the possibility of overwriting and/or adding new custom functions, this library can support arbitrary complex transformation.

## Quick start

For example, the following JSON document:

```json
{
    "a": {
        "value": "value1"
    },
    "b": "value2",
    "c": [
        {
            "values": [
                {
                    "value": "value3"
                },
                {
                    "value": "value4"
                }
            ]
        },
        {
            "values": [
                {
                    "value": "value5"
                },
                {
                    "value": "value6"
                }
            ]
        }
    ],
    "numbers": [
        1,
        2,
        5,
        7
    ]
}

```

Transformed with this transformer:

```json
{
    "transformations": [
        {
            "sourcePointer": "/a/value",
            "resultPointer": "/x"
        },
        {
            "sourcePointer": "/b",
            "resultPointer": "/y"
        },
        {
            "sourcePointer": "/c[i]/values[i]/value",
            "resultPointer": "/z"
        },
        {
            "resultPointer": "/greeting",
            "expressions": [
                "\"Hello, World!\""
            ]
        },
        {
            "sourcePointer": "/numbers",
            "resultPointer": "/total",
            "expressions": [
                "reduce(res = res + x)"
            ]
        }
    ]
}
```

Becomes:

```json
{
    "x": "value1",
    "y": "value2",
    "z": [
        "value3",
        "value4",
        "value5",
        "value6"
    ],
    "greeting": "Hello, World!",
    "total": 15.0
}
```

This can be implemented the following way, using the string representations of the documents in the example above:

```java
    public static final TransformerFactory FACTORY = TransformerFactory.factory();

    public String testTransformer(final String transformerStr, final String sourceStr) {
        final Transformer transformer = FACTORY.createFromJsonString(transformerStr);
        final JsonObject result = transformer.transform(Json.createParser(
            new StringReader(sourceStr)).getObject());
        return result.toString();
    }
```

## Dependencies

This library is implemented using the [Jakarta JSON Processing](https://jakarta.ee/specifications/jsonp/) specification. It also uses (for parsing of the transformers from JSON documents) the [Jakarta JSON Binding](https://jakarta.ee/specifications/jsonb/) specification. Particular implementations of these specifications, which are necessary for running the code of this library, can be chosen at runtime. This project uses (with the dependency scope `provided`) the following implementations: `pkg:maven/org.glassfish/jakarta.json@2.0.1` and `pkg:maven/org.eclipse/yasson@3.0.3`.

Some of the built-in functions provided in this project use JavaScript as expression language. If you are using these function, or you are adding your own functions using JavaScript as expression language, then you need to provide a [jvax.script](https://docs.oracle.com/javase/6/docs/api/javax/script/package-summary.html) implementation. This project uses (with the dependency scope `provided`) the following implementation: `pkg:maven/org.openjdk.nashorn/nashorn-core@15.4`.

## Transformer

Transformer contains only one field `transformations`, which is an array of transformations, each having the following structure:
- boolean `append` (default: `false`): it can only be set to `true` when the JSON value at the `resultPointer` is an array (or that value does not yet exist). In that case, values resulting from this transformation are appended to the array at the `resultPointer` (see [Working with arrays](#working-with-arrays)).
- boolean `useResultAsSource`(default: `false`): when set to `true` the result is also used as the source of this transformation, where the source itself is ignored. It is useful, for example, when using the `filter` function on an array in the resulting document (see [Expressions](#expressions)).
- string `sourcePointer` (default: `""`): a JSON Pointer extended with `[i]` notation (see [Working with arrays](#working-with-arrays)) pointing to a value in the source document.
- string `resultPointer` (default: `""`): a JSON Pointer extended with `[i]` notation (see [Working with arrays](#working-with-arrays)) pointing to a value in the resulting document.
- array of strings `expressions` (empty by default): when not defined (left empty), the transformations copies the value from `sourcePointer` to the `resultPointer`. If the value at the `resultPointer` does not yet exist, it is created. If it already exists, and it is not an array we are appending to (`"append": true`), then the value is merged with the already existing value (see [Merging already existing values](#merging-already-existing-values)). When `expressions` are not empty, then the values are produced according to these expressions (see [Expressions](#expressions)), i.e., they override the default `copy` behavior.

Note that empty string (`""`) is a valid JSON Pointer that points to the whole document. The identity transformation that copies the whole source document to the resulting document can be then created with the following transformer:

```json
{
    "transformations": [
        {}
    ]
}
```

Or in a more verbose way, by filling in the default values:

```json
{
    "transformations": [
        {
            "append": false,
            "useResultAsSource": false,
            "sourcePointer": "",
            "resultPointer": "",
            "expressions": []
        }
    ]
}
```

### Merging already existing values

The default behavior of the library is merging values at the `resultPointer` with values from the `sourcePointer`. This can be overridden by setting the `"append": true` (in that case, values are appended to the array at the `resultPointer`, which is created if it does not yet exist), or by using specific expressions (for example, `remove(/x)` would remove `x` field in the object at the `resultPointer`). This section focuses on the merging behavior.

In the trivial case where there is no value defined in the resulting document at `resultPointer`, the values are inserted at the `resultPointer` path in the source document. Note that that path may not exist yet in the resulting document. In fact, it is always the case at the beginning of the transformation. The objects needed for that path to be valid are then created, and the value is inserted at the right spot. If the value already exists, then it is overwritten. This can be illustrated with the following example.

Source:
```json
{
    "a": "x",
    "b": "y"
}
```

Transformer:
```json
{
    "transformations": [
        {
            "sourcePointer": "/a",
            "resultPointer": "/result1"
        },
        {
            "sourcePointer": "/b",
            "resultPointer": "/result1"
        },
        {
            "sourcePointer": "/a",
            "resultPointer": "/result2/x"
        },
        {
            "sourcePointer": "/b",
            "resultPointer": "/result2/y"
        }
    ]
}
```

Result:
```json
{
    "result1": "y",
    "result2": {
        "x": "x",
        "y": "y"
    }
}
```

The same behavior can be observed in more complex situations, e.g., when merging objects inside (possibly nested) arrays of objects. Also, objects in flattened array or in arrays of different lengths can be merged that way. If the array is shorter, or even empty, new objects are created at the corresponding (not yet existing) positions. Existing objects themselves are always merged the same way, just as described above. However, the value at the `resultPointer` could be either a simple value, or a JSON structure, like an array or an object. For example, if the already existing value being merged is an array, it will be overwritten with the new value (array or otherwise and vice versa). For example:

Source:
```json
{
    "a": [1, 2, 3],
    "b": "y"
}
```

Transformer (the same as in the example above):
```json
{
    "transformations": [
        {
            "sourcePointer": "/a",
            "resultPointer": "/result1"
        },
        {
            "sourcePointer": "/b",
            "resultPointer": "/result1"
        },
        {
            "sourcePointer": "/a",
            "resultPointer": "/result2/x"
        },
        {
            "sourcePointer": "/b",
            "resultPointer": "/result2/y"
        }
    ]
}
```

Result:
```json
{
    "result1": "y",
    "result2": {
        "x": [
            1,
            2,
            3
        ],
        "y": "y"
    }
}
```

If we want to treat each element of an array as a separate value, then we need to use the `[i]` notation and iterate over the values inside that array (see also [Working with arrays](#working-with-arrays)). For example:

Source:
```json
{
    "a": [1, 2, 3],
    "b": ["a", "b", "c"]
}
```

Transformer:
```json
{
    "transformations": [
        {
            "sourcePointer": "/a[i]",
            "resultPointer": "/result[i]/x"
        },
        {
            "sourcePointer": "/b[i]",
            "resultPointer": "/result[i]/y"
        }
    ]
}
```

Result:
```json
{
    "result": [
        {
            "x": 1,
            "y": "a"
        },
        {
            "x": 2,
            "y": "b"
        },
        {
            "x": 3,
            "y": "c"
        }
    ]
}
```

As can be seen, the resulting objects are merged in a consistent manner, just as described before. More details on using the `[i]` notation for iterating over elements in an array can be found in [Working with arrays](#working-with-arrays).

### Expressions

Expressions can be either string [literals](#literals) or calls to [functions](#functions) registered in the transformer factory. Each transformation in the transformer can have multiple expressions, they are then executed in the order that they are defined. Note that when JavaScript code is used in the expressions, you can store variables in the script engine, and these variables become accessible in all the following expressions, also in the expressions from the transformations that are defined after the transformation where the variable is stored. The execution engine is then shared over the whole transform action. It created in a lazy manner, meaning that when no expressions using the JavaScript are defined in a transformer, the engine is never created.

#### Literals

All expressions starting with an escaped quotation mark (`\"`) are treated by this library as string literals. String literals are placed between `\"\"` and the value of them is copied to the value at the `resultPointer` (`sourcePointer` is ignored by the expressions containing only literals of any type). Only the string literals are supported this way and other types of literals, as shown in the example below, can be expressed in the `script` function as JavaScript literals. Notice the wrapping of the JavaScript array literal between `[]` in the `List` type (that is mapped to the `java.util.ArrayList` Java type, as explained in the [next section](#java-types-inside-the-javascript-expressions)). This wrapping is needed when using the `nashorn` implementation of the script engine, as it interprets all objects from the engine as `java.util.Map` instances, unless they are mapped to another Java type, as it is the case for the `List` type. The primitive JavaScript types, like `int` or `string`, as well as object literals, are processed as expected and do not need any special mapping. This is further illustrated in the following example:

Transformer:
```json
{
    "transformations": [
        {
            "resultPointer": "/greeting",
            "expressions": [
                "\"Hello, World!\""
            ]
        },
        {
            "resultPointer": "/result",
            "expressions": [
                "script(res = { string: 'Hello!', int: 5, decimal: 1.2, object: { a: 'x', b: 'y' }, array: new List([1, 2, 3]) })"
            ]
        }
    ]
}
```

Result:
```json
{
    "greeting": "Hello, World!",
    "result": {
        "string": "Hello!",
        "int": 5,
        "decimal": 1.2,
        "object": {
            "a": "x",
            "b": "y"
        },
        "array": [
            1,
            2,
            3
        ]
    }
}
```

#### Java types inside the JavaScript expressions

All [functions](#functions) that use JavaScript have access to the Java types mapped to the `Map`, `Set` and `List` types in JavaScript at the moment of the creation of the Script Engine:

```java
    engine.eval("Map = Java.type('java.util.LinkedHashMap')");
    engine.eval("Set = Java.type('java.util.LinkedHashSet')");
    engine.eval("List = Java.type('java.util.ArrayList')");
```

You can override these types, or add any Java type you need by simply adding an expression. For example:

```json
{
    "transformations": [
        {
            "expressions": [
                "script(Date = Java.type('java.util.Date'))"
            ]
        }
    ]
}
```

Note that the expression above does not set the `res` variable to any value. This means that it only has an effect on the engine being used and does not produce any value that can be used in the transformation. In this situation, the transformer simply continues its execution and the particular expression has no effect on the resulting document (it has only an effect on the engine within the scope of the transform action of the transformer).

#### Functions

Expressions that are not starting with `\"` are treated as calls to functions that are mapped in the transformer factory. The syntax of such expression is the name of the function followed by the argument(s) between `()`, which might be left empty, depending on the definition of the function that is being called. This library provides several bult-in functions:
- `copy(\fromPointer, \toPointer)`
- `move(\fromPointer, \toPointer)`
- `remove(\atPointer)`
- `generateUuid(\atPointer)`
- `script(res = myFunction(x))`
- `filter(res = x > 2)`
- `map(res = { a: x.field1, b: x.field2 })`
- `reduce(res = res + x)`

### Working with arrays

### Example

## Thread safety

Thread safety using this library is achieved by the concepts of immutability, and no synchronization is needed when using this library. The only mutable objects that are possibly exposed during the transformations are the JavaScript Engine instances. However, they are created for each transformation separately and should not be used outside that transformation scope.
