# JaVar #

A library providing an expressive syntax for defining Java variable values from multiple potential
inputs, some of which may be null.  It offers a succinct replacement for the following
difficult-to-read boilerplate patterns:

```java
void init(@Nullable String prop) {
  this.prop = prop;
  if (this.prop == null) {
    this.prop = lookupPropFromDb(); // slow
  }
  if (this.prop == null) {
    this.prop = DEFAULT_PROP_VALUE;
  }
  if (this.prop == null) {
    throw new IllegalStateException();
  }
}
```

...or the "simpler" variant:

```java
String str = firstVal != null ? firstVal : secondVal != null ? secondVal : null;
```

(the latter example is especially problematic when, for example, `firstVal` is
actually `myObj.getFirstVal()`: one must either assign it to a variable beforehand,
or trust that `getFirstVal()` is a fast method that will return the same value
when called multiple times)

In the absence of first-class support for this pattern in the language, this class offers an
awkward but relatively elegant solution:

```java
void init(@Nullable String prop) {
  Var<String> var = Var.var();
  this.prop = var.def(
    var.as(prop)
    || var.as(lookupPropFromDb())
    || var.as(System.getenv("PROP")));
}
```

In the above example, `lookupPropFromDb()` is only called if `prop` is
null.  Also, if all three potential values are null, it will throw an `IllegalStateException`
to indicate that the variable is undefined.

```java
Var<String> var = Var.var();
String str = var.def(var.as(firstVal) || var.as(secondVal) || var.asNull());
```

The call to `var.asNull()` allows the variable to contain an explicitly null value,
without triggering an `IllegalStateException`.

