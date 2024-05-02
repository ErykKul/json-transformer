last = x.path.split('/').slice(-1);
id = '';
parent = '';
list.addAll(x.path.split('/').map(function (p) {
    id = id + '/' + p;
    r = { id: id, name: p, parent: parent, isDir: p != last };
    parent = parent + '/' + p;
    return r;
}));
