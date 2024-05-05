# JSON Transformer

JSON Transformer is a simple and expressive Java library that uses [transformers](#transformer) defined in JSON documents for transformations of JSON structures (even complex structures containing nested arrays of objects) into other (complex) JSON structures. At its bases, it uses the [JavaScript Object Notation (JSON) Pointers](https://datatracker.ietf.org/doc/html/rfc6901) specification, as provided by the `jakarta.json` API. This library extends that specification with the `[i]` notation for [working with arrays](#working-with-arrays). It also provides several built-in [functions](#functions), ranging from the very basic `copy` function taking two JSON pointers as arguments, to more expressive `map`, `filter` and `reduce` functions taking JavaScript expressions as arguments (e.g., `res = res + x`). Together with the `script` function, that can be used to produce any JSON object (e.g., `res = { a: 1, b: myFunc(x) }`), and the possibility of overwriting and/or adding new custom functions, this library can support arbitrary complex transformation. Finally, you can also use the [literals](#literals) for addind static information to your transformed JSON documents.

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
    ],
    "strings": ["a", "b", "c"]
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
            "resultPointer": "/y/z"
        },
        {
            "sourcePointer": "/numbers[i]",
            "resultPointer": "/merged[i]/x"
        },
        {
            "sourcePointer": "/strings[i]",
            "resultPointer": "/merged[i]/y"
        },
        {
            "resultPointer": "/stringLiteral",
            "expressions": [
                "\"Hello, World!\""
            ]
        },
        {
            "resultPointer": "/JSliterals",
            "expressions": [
                "script(res = { string: 'Hello!', int: 5, decimal: 1.2, object: { a: 'x', b: 'y' }, array: new List([1, 2, 3]) })"
            ]
        },
        {
            "expressions": [
                "copy(/b, /copied)",
                "copy(/b, /temp)",
                "move(/temp, /moved)",
                "generateUuid(/uuid)"
            ]
        },
        {
            "resultPointer": "/scriptResult",
            "expressions": [
                "script(concat = function (c, n) { return (c ? c + ', ' : '') + n })",
                "script(res = { concat: x.strings.stream().reduce(null, concat) })"
            ]
        },
        {
            "sourcePointer": "/numbers",
            "resultPointer": "/filtered",
            "expressions": [
                "filter(res = x > 2)"
            ]
        },
        {
            "sourcePointer": "/numbers",
            "resultPointer": "/mapped",
            "expressions": [
                "map(res = x + 5)"
            ]
        },
        {
            "sourcePointer": "/numbers",
            "resultPointer": "/total",
            "expressions": [
                "reduce(res = res + x)"
            ]
        },
        {
            "expressions": [
                "withLogger(remove(/uuid))"
            ]
        },
        {
            "sourcePointer": "/numbers",
            "resultPointer": "/concat",
            "expressions": [
                "reduce(res = (res ? res + ', ' : '') + x)"
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
    "merged": [
        {
            "x": 1,
            "y": "a"
        },
        {
            "x": 2,
            "y": "b"
        },
        {
            "x": 5,
            "y": "c"
        },
        {
            "x": 7
        }
    ],
    "stringLiteral": "Hello, World!",
    "JSliterals": {
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
    },
    "copied": "value2",
    "moved": "value2",
    "scriptResult": {
        "concat": "a, b, c"
    },
    "filtered": [
        5,
        7
    ],
    "mapped": [
        6.0,
        7.0,
        10.0,
        12.0
    ],
    "total": 15.0,
    "concat": "1, 2, 5, 7"
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

## Table of Contents

- [Dependencies](#dependencies)
- [Transformer](#transformer)
    - [Merging already existing values](#merging-already-existing-values)
    - [Expressions](#expressions)
        - [Literals](#literals)
        - [Java types inside the JavaScript expressions](#java-types-inside-the-javascript-expressions)
        - [Functions](#functions)
        - [Importing JavaScript files](#importing-javascript-files)
    - [Working with arrays](#working-with-arrays)
        - [Using the `append` transformation field](#using-the-append-tranformation-field)
        - [Iterating over arrays with the `[i]` notation](#iterating-over-arrays-with-the-i-notation)
        - [Note on accessing parent objects](#note-on-accessing-parent-objects)
- [Running the examples](#running-the-examples)
- [Thread safety](#thread-safety)

## Dependencies

This library is implemented using the [Jakarta JSON Processing](https://jakarta.ee/specifications/jsonp/) specification. It also uses (for parsing of the transformers from JSON documents) the [Jakarta JSON Binding](https://jakarta.ee/specifications/jsonb/) specification. Particular implementations of these specifications, which are necessary for running the code of this library, can be chosen at runtime. This project uses (with the dependency scope `provided`) the following implementations: `pkg:maven/org.glassfish/jakarta.json@2.0.1` and `pkg:maven/org.eclipse/yasson@3.0.3`.

Some of the built-in functions provided in this project use JavaScript as expression language. If you are using these function, or you are adding your own functions using JavaScript as expression language, then you need to provide a [jvax.script](https://docs.oracle.com/javase/6/docs/api/javax/script/package-summary.html) implementation. This project uses (with the dependency scope `provided`) the following implementation: `pkg:maven/org.openjdk.nashorn/nashorn-core@15.4`.

## Transformer

Transformer contains only one field `transformations`, which is an array of transformations, each having the following structure:
- boolean `append` (default: `false`): it can only be set to `true` when the JSON value at the `resultPointer` is an array (or that value does not yet exist). In that case, values resulting from this transformation are appended to the array at the `resultPointer` (see [working with arrays](#working-with-arrays)).
- boolean `useResultAsSource`(default: `false`): when set to `true` the result is also used as the source of this transformation, where the source itself is ignored. It is useful, for example, when using the `filter` function on an array in the resulting document (see [functions](#functions)).
- string `sourcePointer` (default: `""`): a JSON Pointer extended with `[i]` notation (see [working with arrays](#working-with-arrays)) pointing to a value in the source document.
- string `resultPointer` (default: `""`): a JSON Pointer extended with `[i]` notation (see [working with arrays](#working-with-arrays)) pointing to a value in the resulting document.
- array of strings `expressions` (empty by default): when not defined (left empty), the transformations copies the value from `sourcePointer` to the `resultPointer`. If the value at the `resultPointer` does not yet exist, it is created. If it already exists, and it is not an array we are appending to (`"append": true`), then the value is merged with the already existing value (see [merging already existing values](#merging-already-existing-values)). When `expressions` are not empty, then the values are produced according to these expressions (see [](#expressions)), i.e., they override the default `copy` behavior and can be either [literas](#literals) or calls to [functions](#functions).

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

The remainder of this section is structured as follows:
- [Merging already existing values](#merging-already-existing-values)
- [Expressions](#expressions)
    - [Literals](#literals)
    - [Java types inside the JavaScript expressions](#java-types-inside-the-javascript-expressions)
    - [Functions](#functions)
    - [Importing JavaScript files](#importing-javascript-files)
- [Working with arrays](#working-with-arrays)
    - [Using the `append` transformation field](#using-the-append-tranformation-field)
    - [Iterating over arrays with the `[i]` notation](#iterating-over-arrays-with-the-i-notation)
    - [Note on accessing parent objects](#note-on-accessing-parent-objects)

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

If we want to treat each element of an array as a separate value, then we need to use the `[i]` notation and iterate over the values inside that array (see also [working with arrays](#working-with-arrays)). For example:

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

As can be seen, the resulting objects are merged in a consistent manner, just as described before. More details on using the `[i]` notation for iterating over elements in an array can be found in [working with arrays](#working-with-arrays) section.

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

Expressions that are not starting with `\"` are treated as calls to functions that are registered in the transformer factory. The syntax of such an expression is the name of the function followed by the argument(s) between round brackets (`()`), that might be left empty, depending on the definition of the function that is being called. This library provides several built-in functions:
- `copy(/fromPointer, /toPointer)`: copies a value from the `/fromPointer` (relative to the `sourcePointer`) in the source document to the `/toPointer` (relative to the `resultPointer`) in the resulting document. Notice that it is very similar to the default copy functionality when the expressions of the transformation are left empty. In fact, the default functionality is identical with `copy(, )` (or simply `copy()`, where both; the `/fromPointer` and the `/toPointer` are empty string pointers, and the value is copied from the `/sourcePointer` to the `resultPointer`).
- `move(/fromPointer, /toPointer)`: moves a value from the `/fromPointer` (relative to the `sourcePointer`) to the `/toPointer` (relative to the `resultPointer`) in the resulting document (the source document is ignored by this function).
- `remove(/atPointer)`: removes a value from the `/atPointer` (relative to the `resultPointer`) in the resulting document. The `\atPointer` cannot be an empty string pointer, as remove operations are not permitted on the root.
- `generateUuid(/atPointer)`: generates a UUID at the `/atPointer` (relative to the `resultPointer`) in the resulting document.
- `script(res = myFunction(x))`: executes the JavaScript script sent as an argument to this function. If the script writes a value to the `res` variable, that value is written at the `resultPointer` in the resulting document.
- `filter(res = x > 2)`: filters out values from an array (or fields in an object) at the `sourcePointer` in the source document that do not produce `res = true` in the JavaScript script provided as argument to this function. The values or fields being filtered are passed as `x` variables to the script engine by the library. The result of the expression is written at the `resultPointer` in the resulting document.
- `map(res = { a: x.field1, b: x.field2 })`: maps values from an array (or fields in an object) at the `sourcePointer` in the source document to the values written in the `res` variable by the JavaScript script provided as argument to this function. The values or fields being mapped are passed as `x` variables to the script engine by the library. The result of the expression is written at the `resultPointer` in the resulting document.
- `reduce(res = res + x)`: reduces values from an array (or fields in an object) at the `sourcePointer` in the source document to the values written in the `res` variable by the JavaScript script provided as argument to this function. The values or fields being reduced are passed as `x` variables to the script engine by the library. The result of the expression is written at the `resultPointer` in the resulting document.

You can add functions (or even overwrite the built-in functions) to the transformer factory by implementing the `ExprFunction` functional interface and registering it in the transformer factory. For example, if you want to add logging for debugging purposes to the execution of an expression, you may write a code similar to the following function:

```java
    public static final ExprFunction LOGGER = (ctx, source, result, expression) -> {
        System.out.println("*****\n");
        System.out.println("ctx -> " + ctx.toJsonObject() + "\n");
        System.out.println("source -> " + source + "\n");
        System.out.println("result -> " + result + "\n");
        System.out.println("expression -> " + expression + "\n");
        final List<String> expressions = new ArrayList<>();
        if (expression != null && !"".equals(expression)) {
            expressions.add(expression);
        }
        final JsonValue res = Transformation.executeExpressions(ctx, source, result, expressions);
        System.out.println("res -> " + res + "\n");
        System.out.println("*****");
        return res;
    };
```

The `source` and the `result` JsonValues are the values at the `sourcePointer` in the source document and the `resultPointer` in the resulting document, respectively. The `expression` is the string value between the round brackets (`()`) that is passed to this function in the expression of the transformation being executed. The `ctx` is the transformation context containing, a.o., the script engine constructed for the execution of the transform action of the transformer. You can also retrieve the `useResultAsSource` value from that context, which would indicate if you need to use the result as source, and then ignore the source document. It also provides access to the map of functions registered in the transformer factory. Other fields, namely `globalSource`, `globalResult`, `localSource` and `localResult`, are used mainly by the  framework itself and can be ignored for other than debugging purposes. You can then make that new function available to the expressions in the transformer by creating a new transformer factory with that new function registered (the built-in functions are registered as well in that factory):
```java
public static final TransformerFactory FACTORY_WITH_LOGGER = TransformerFactory
        .factory(Map.of("withLogger", LOGGER));
```

You can then create new transformers using that new factory, e.g.:
```java
final Transformer transformer = FACTORY_WITH_LOGGER.createFromFile("example/transformer.json");
```

Transformers created that way can then access the new function. For example:
```json
{
    "transformations": [
        {
            "resultPointer": "/expressionsResults",
            "expressions": [
                "withLogger(generateUuid(/uuid))",
            ]
        }
    ]
}
```

The following example illustrates the usage of the functions as described in this section:

Source:
```json
{
    "a": [1, 2, 3],
    "b": "y"
}
```

Tranformer:
```json
{
    "transformations": [
        {
            "expressions": [
                "copy(/b, /copied)",
                "copy(/b, /temp)",
                "move(/temp, /moved)",
                "generateUuid(/uuid)"
            ]
        },
        {
            "resultPointer": "/scriptResult",
            "expressions": [
                "script(concat = function (c, n) { return (c ? c + ', ' : '') + n })",
                "script(res = { concat: x.a.stream().reduce(null, concat) })"
            ]
        },
        {
            "sourcePointer": "/a",
            "resultPointer": "/filtered",
            "expressions": [
                "filter(res = x > 1)"
            ]
        },
        {
            "sourcePointer": "/a",
            "resultPointer": "/mapped",
            "expressions": [
                "map(res = x + 5)"
            ]
        },
        {
            "sourcePointer": "/a",
            "resultPointer": "/reduced",
            "expressions": [
                "reduce(res = res + x)"
            ]
        },
        {
            "expressions": [
                "withLogger(remove(/uuid))"
            ]
        },
        {
            "sourcePointer": "/a",
            "resultPointer": "/concat",
            "expressions": [
                "reduce(res = (res ? res + ', ' : '') + x)"
            ]
        }
    ]
}
```

Result:
```json
{
    "copied": "y",
    "moved": "y",
    "scriptResult": {
        "concat": "1, 2, 3"
    },
    "filtered": [
        2,
        3
    ],
    "mapped": [
        6.0,
        7.0,
        8.0
    ],
    "reduced": 6.0,
    "concat": "1, 2, 3"
}
```

Where we can also see the debug output from the `withLogger` function in the console:
```
*****

ctx -> {"transformation":{"append":false,"useResultAsSource":false,"sourcePointer":"","resultPointer":"","expressions":["withLogger(remove(/uuid))"]},"globalSource":{"a":[1,2,3],"b":"y"},"globalResult":{"copied":"y","moved":"y","uuid":"8605858c-4e95-47bb-aa4d-afc1af46e354","scriptResult":{"test":"123"},"filtered":[2,3],"mapped":[6.0,7.0,8.0],"reduced":6.0},"localSource":{"a":[1,2,3],"b":"y"},"localResult":{"copied":"y","moved":"y","uuid":"8605858c-4e95-47bb-aa4d-afc1af46e354","scriptResult":{"test":"123"},"filtered":[2,3],"mapped":[6.0,7.0,8.0],"reduced":6.0}}

source -> {"a":[1,2,3],"b":"y"}

result -> {"copied":"y","moved":"y","uuid":"8605858c-4e95-47bb-aa4d-afc1af46e354","scriptResult":{"test":"123"},"filtered":[2,3],"mapped":[6.0,7.0,8.0],"reduced":6.0}

expression -> remove(/uuid)

res -> {"copied":"y","moved":"y","scriptResult":{"test":"123"},"filtered":[2,3],"mapped":[6.0,7.0,8.0],"reduced":6.0}

*****
```

#### Importing JavaScript files

In certain situations, a more complex JavaScript script may be needed to achieve the desired transformation. This kind of script becomes unreadable when written on one line (e.g., using semicolon (`;`) for splitting the script lines). Let's look, for example, at the following JSON document:

Source:
```json
{
    "files": [
        {
            "path": "file.txt"
        },
        {
            "path": "a/file1.txt"
        },
        {
            "path": "b/file1.txt"
        },
        {
            "path": "c/file1.txt"
        },
        {
            "path": "a/ab/file1.txt"
        }
    ]
}
```

If the desired transformation would be having the files in a graph representation, where each object has a name (either a file name or a directory name), a reference to its parent directory, and a boolean indicating if it is a directory, then you might end up writing the following transformer:

Transformer:
```json
{
    "transformations": [
        {
            "sourcePointer": "/files",
            "resultPointer": "/graph",
            "expressions": [
                "script(list = new List())",
                "map(last = x.path.split('/').slice(-1); id = ''; parent = ''; list.addAll(x.path.split('/').map(function (p) { id = id + '/' + p; r = { id: id, name: p, parent: parent, isDir: p != last }; parent = parent + '/' + p; return r; }));)",
                "script(res = list)"
            ]
        },
        {
            "useResultAsSource": true,
            "sourcePointer": "/graph",
            "resultPointer": "/graph",
            "expressions": [
                "script(set = new Set())",
                "filter(res = set.add(x))",
                "map(if (x.parent == '') delete x.parent; res = x)"
            ]
        }
    ]
}
```

The long `map` expression in the first transformation is not easily readable in that transformer. The script inside that expression becomes much more readable when stored in a separate file:

JavaScript stored in [/example/split.paths](/examples/split_paths.js)
```javascript
last = x.path.split('/').slice(-1);
id = '';
parent = '';
list.addAll(x.path.split('/').map(function (p) {
    id = id + '/' + p;
    r = { id: id, name: p, parent: parent, isDir: p != last };
    parent = parent + '/' + p;
    return r;
}));
```

We can then create a transformer that imports that file inside the expression using the `importJS {fileName} endImport` notation, as show in the example below:

Transformer with JavaScript file import:
```json
{
    "transformations": [
        {
            "sourcePointer": "/files",
            "resultPointer": "/graph",
            "expressions": [
                "script(list = new List())",
                "map(importJS examples/split_paths.js endImport)",
                "script(res = list)"
            ]
        },
        {
            "useResultAsSource": true,
            "sourcePointer": "/graph",
            "resultPointer": "/graph",
            "expressions": [
                "script(set = new Set())",
                "filter(res = set.add(x))",
                "map(if (x.parent == '') delete x.parent; res = x)"
            ]
        }
    ]
}
```

The new transformation becomes much more readable that way. When we execute it on the source document from the example above, we get the following resulting document:

Result:
```json
{
    "graph": [
        {
            "id": "/file.txt",
            "name": "file.txt",
            "isDir": false
        },
        {
            "id": "/a",
            "name": "a",
            "isDir": true
        },
        {
            "id": "/a/file1.txt",
            "name": "file1.txt",
            "parent": "/a",
            "isDir": false
        },
        {
            "id": "/b",
            "name": "b",
            "isDir": true
        },
        {
            "id": "/b/file1.txt",
            "name": "file1.txt",
            "parent": "/b",
            "isDir": false
        },
        {
            "id": "/c",
            "name": "c",
            "isDir": true
        },
        {
            "id": "/c/file1.txt",
            "name": "file1.txt",
            "parent": "/c",
            "isDir": false
        },
        {
            "id": "/a/ab",
            "name": "ab",
            "parent": "/a",
            "isDir": true
        },
        {
            "id": "/a/ab/file1.txt",
            "name": "file1.txt",
            "parent": "/a/ab",
            "isDir": false
        }
    ]
}
```

### Working with arrays

This section is structured as follows:
- [Using the `append` transformation field](#using-the-append-tranformation-field)
- [Iterating over arrays with the `[i]` notation](#iterating-over-arrays-with-the-i-notation)
- [Note on accessing parent objects](#note-on-accessing-parent-objects)

#### Using the `append` transformation field

We can append to an array at the `resultPointer` in the resulting document by setting the `"append": true`. If that array does not yet exist (e.g., on the first append call), then that array is created. Appending to an array is illustrated in the following example:

Source:
```json
{
    "a": {
        "x": "1"
    },
    "b": "y",
    "c": [
        1,
        2,
        2
    ]
}
```

Transformer:
```json
{
    "transformations": [
        {
            "append": true,
            "sourcePointer": "/a",
            "resultPointer": "/appended"
        },
        {
            "append": true,
            "sourcePointer": "/b",
            "resultPointer": "/appended"
        },
        {
            "append": true,
            "sourcePointer": "/c",
            "resultPointer": "/appended"
        },
        {
            "append": true,
            "resultPointer": "/appended",
            "expressions": [
                "\"literal\""
            ]
        }
    ]
}
```

Result:
```json
{
    "appended": [
        {
            "x": "1"
        },
        "y",
        [
            1,
            2,
            2
        ],
        "literal"
    ]
}
```

#### Iterating over arrays with the `[i]` notation

#### Note on accessing parent objects

## Running the examples

All the examples from this documentation are provided as test cases. If you wish to run them yourself and experiment with this library, you can check out this repository and run the tests from the [TransformerTest.java](/src/test/java//io/github/erykkul/json/transformer/TransformerTest.java) class by running the `mvn test` command.

The examples themselves can be found in the [examples](/examples/) directory, that next to JSON files from the examples in this documentation in the [documentation](/examples/documentation/) directory, contains an extra example and the [split_paths.js](/examples/split_paths.js) file, as used in the [importing javascript files](#importing-javascript-files) section.

## Thread safety

Thread safety using this library is achieved by the concepts of immutability, and no synchronization is needed when using this library. The only mutable objects that are possibly exposed during the transformations are the JavaScript Engine instances. However, they are created for each transformation separately and should not be used outside that transformation scope.
