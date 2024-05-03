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

## Transformation structure
