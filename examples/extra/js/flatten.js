doFlatten = function (v) {
    if (v.add && v.size() > 0) {
        if (v.size() > 1) {
            return v.stream().map(doFlatten).collect(Collectors.toList());
        }
        return doFlatten(v.get(0));
    } else if (v.keySet) {
        var m = new Map();
        v.keySet().forEach(flatten(m, v));
        return m;
    }
    return v;
}

var empty = new List();

flatten = function (result, value) {
    return function (key) {
        var v = value.get(key)
        if (v !== null && !JsonValue.NULL.equals(v) && !empty.equals(v)) {
            result.put(key, doFlatten(v));
        }
    }
};

res = new Map();
x.keySet().forEach(flatten(res, x))