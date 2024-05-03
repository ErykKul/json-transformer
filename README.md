# JSON Transformer

JSON Transformer is a simple and expressive Java library that uses transformations defined in a JSON document for transforming JSON structures, e.g., complex structures containing nested arrays of objects, into other (complex) JSON structures. At its bases it uses [JavaScript Object Notation (JSON) Pointers](https://datatracker.ietf.org/doc/html/rfc6901) (as already supported by the `jakarta.json` API) for selecting values, extended with `[i]` notation for working with arrays. In the most simple case, a value from the `sourcePointer` in the source document is copied to a (new) value at the `resultPointer` in the resulting document. It also provides several built-in functions, ranging from the very basic `copy` function taking two pointers as arguments, to more expressive `map`, `filter` and `reduce` functions taking simple JavaScript expressions (e.g., `res = res + x`). Together with the `script` function, that can be used to produce any JSON object (e.g., `res = { a: 1, b: myFunc(x) }`), and the possibility of overwriting and/or adding any custom functions, this library allows arbitrary complex transformation.

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

Which can be implemented the following way:

```java
    public static final TransformerFactory FACTORY = TransformerFactory.factory();

    public String testTransformer(final String transformerAsString, final String sourceAsString) {
        final Transformer transformer = FACTORY.createFromJsonString(transformerAsString);
        final JsonObject result = transformer.transform(Json.createParser(new StringReader(sourceAsString)).getObject());
        return result.toString();
    }
```

Passing the transformer and the source document from the exmaple above to that method produces the string representation of the result.

## Dependencies

This library is implemented using [Jakarta JSON Processing](https://jakarta.ee/specifications/jsonp/) specification. It also uses the [Jakarta JSON Binding](https://jakarta.ee/specifications/jsonb/) specification for parsing of the transformations from JSON documents. Specific implementations of these specifications, which are necessary for running the code of this library, can be chosen at runtime. This project uses (dependency scope `provided`) the following implementations: `pkg:maven/org.glassfish/jakarta.json@2.0.1` and `pkg:maven/org.eclipse/yasson@3.0.3`.

Some of the built-in functions provided in this project use JavaScript as expression language. If you are using these function, or you are adding your own functions using JavaScript as expression language, then you need to provide a [jvax.script](https://docs.oracle.com/javase/6/docs/api/javax/script/package-summary.html) implementation. This project uses (dependency scope `provided`) the following implementation: `pkg:maven/org.openjdk.nashorn/nashorn-core@15.4`.

## Transformer

Transformer contains only one field `transformations`, which is an array of transformations, each having the following structure:
- boolean `append` (default: `false`): it can only be set to `true` when the JsonValue at the `resultPointer` is an array (or that value does not yet exist). In that case, values resulting from this transformation are appended to the array at the `resultPointer` (see [Working with arrays](#working-with-arrays)).
- boolean `useResultAsSource`(default: `false`): when set to `true` the result is also used as the source of this transformation, where the source itself is ignored. It is useful, for example, when using the `filter` function on an array in the resulting document (see [Expressions](#expressions)).
- string `sourcePointer` (default: `""`): a JSON Pointer extended with `[i]` notation (see [Working with arrays](#working-with-arrays)) pointing to a value in the source document.
- string `resultPointer` (default: `""`): a JSON Pointer extended with `[i]` notation (see [Working with arrays](#working-with-arrays)) pointing to a value in the resulting document.
- array of strings `expressions` (empty by default): when not defined (left empty), the transformations copies the value from `sourcePointer` to the `resultPointer`. If the value at the `resultPointer` does not yet exist, it is created. If it already exists, and it is not an array we are appending to (`"append": true`, see also [Working with arrays](#working-with-arrays)), then the value is merged with the already existing value (see [Merging already existing values](#merging-already-existing-values)). When `expressions` are not empty, then the values are produced according to these expressions (see [Expressions](#expressions)).

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

The same behavior can be observed in more complex situations when merging objects inside (possibly nested) arrays of objects. Also, objects in flattened array or in arrays of different lengths can be merged that way. If the array is shorter, or even empty, new objects are created at the corresponding (not yet existing) positions. Existing objects are always merged the same way, just as described above. However, if the value being merged is itself an array, it is overwritten with the new value (array or otherwise and vice versa). For example:

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
```json

Result:
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

This behavior can be changed, as described in [Working with arrays](#working-with-arrays), by using the `[i]` notation and iterating over the values inside an array. For example:

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

More details on using the `[i]` notation for iterating over elements in an array can be found in [Working with arrays](#working-with-arrays).

### Expressions

### Working with arrays

### Example