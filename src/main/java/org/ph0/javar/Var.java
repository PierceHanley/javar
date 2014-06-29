package org.ph0.javar;

import java.util.List;
import java.util.function.Supplier;

/**
 * Utility class representing a mutable value.  It offers a succinct replacement for the following
 * difficult-to-read boilerplate patterns:
 * 
 * <pre><code>
 * void init(@Nullable String prop) {
 *   this.prop = prop;
 *   if (this.prop == null) {
 *     this.prop = lookupPropFromDb(); // slow
 *   }
 *   if (this.prop == null) {
 *     this.prop = DEFAULT_PROP_VALUE;
 *   }
 *   if (this.prop == null) {
 *     throw new IllegalStateException();
 *   }
 * }
 * </code></pre>
 * 
 * ...or the "simpler" variant:
 * 
 * <pre><code>
 * String str = firstVal != null ? firstVal : secondVal != null ? secondVal : null;
 * </code></pre>
 * 
 * (the latter example is especially problematic when, for example, "<code>firstVal</code>" is
 * actually "<code>myObj.getFirstVal()</code>": one must either assign it to a variable beforehand,
 * or trust that "<code>getFirstVal()</code>" is a fast method that will return the same value
 * when called multiple times)
 * 
 * In the absence of first-class support for this pattern in the language, this class offers an
 * awkward but relatively elegant solution:
 * 
 * <pre><code>
 * void init(@Nullable String prop) {
 *   Var<String> var = Var.var();
 *   this.prop = var.def(var.as(prop) || var.as(lookupPropFromDb()) || var.as(System.getenv("PROP")));
 * }
 * </pre></code>
 * 
 * In the above example, "<code>lookupPropFromDb()</code>" is only called if <code>prop</code> is
 * null.  Also, if all three potential values are null, it will throw {@link IllegalStateException}
 * to indicate that the variable is undefined.
 * 
 * <pre><code>
 * Var<String> var = Var.var();
 * String str = var.def(var.as(firstVal) || var.as(secondVal) || var.asNull());
 * </code></pre>
 * 
 * The call to "<code>var.asNull()</code> allows the variable to contain an explicitly null value,
 * without triggering an {@link IllegalStateException}.
 * 
 * @author phanley
 *
 * @param <T> The object type of the variable.
 */
public final class Var<T> implements Supplier<T> {
  public static <S> Var<S> var() {
    return new Var<S>();
  }
  
  /**
   * Create a new variable with the specified initial value.
   * 
   * @param val the initial value of the variable.
   * @return
   */
  public static <S> Var<S> var(S val) {
    return new Var<S>(val);
  }

  private T val;

  /**
   * Instantiate a new variable with no initial value (indistinguishable from a value of null).
   */
  private Var() {
    this.val = null;
  }

  /**
   * Instantiate a new variable with the specified value.
   * 
   * @param val the initial value of the variable.
   */
  private Var(T val) {
    this.val = val;
  }

  /**
   * Get the current value of the variable, or null if it has not been defined.
   * 
   * @return the current value of the variable (which may be null), or null if it has not yet been
   * defined.
   */
  public final T value() {
    return val;
  }

  /**
   * Return the current value of the variable, if it has been defined.
   * 
   * @param defined whether the variable should be considered "defined", regardless of its value.
   * @return the value of the variable.
   * @throws IllegalStateException if the parameter is false, indicating that the variable was not
   *         defined.
   */
  public final T def(boolean defined) {
    if (!defined) {
      throw new IllegalStateException("Variable was not defined.");
    }
    return this.val;
  }
  
  /**
   * Return whether the variable already has a defined non-null value.
   * 
   * @return whether the variable already has a defined non-null value.
   */
  public final boolean asIs() {
    return this.val != null;
  }

  /**
   * Set the variable to be null.
   * @return true
   */
  public final boolean asNull() {
    this.val = null;
    return true;
  }

  /**
   * Assign a value to the variable.
   * 
   * @param val The new value of the variable.
   * @return true if the new value is not null.
   */
  public final boolean as(T val) {
    this.val = val;
    return this.val != null;
  }

  /**
   * Assign an array element to the variable, if present.
   * 
   * @param vals an array of values, may be null.
   * @param index the index of the desired element within the array.
   * @return true if the array contained a non-null value at the specified index.  Returns false
   * if the array itself is null, or if the index is out of bounds.
   */
  public final boolean asElem(T[] vals, int index) {
    if (vals == null || index < 0 || index >= vals.length) {
      this.val = null;
      return false;
    }
    else {
      this.val = vals[index];
      return this.val != null;
    }
  }

  /**
   * Assign a list element to the variable, if present.
   * 
   * @param vals a {@link List} of values, may be null.
   * @param index the index of the desired element within the array.
   * @return true if the array contained a non-null value at the specified index.  Returns false
   * if the list itself is null, or if the index is out of bounds.
   */
  public final boolean asElem(List<? extends T> vals, int index) {
    if (vals == null || index < 0 || index >= vals.size()) {
      this.val = null;
      return false;
    }
    else {
      this.val = vals.get(index);
      return this.val != null;
    }
  }

  /**
   * Sets the variable to the first non-null value in the specified arguments.
   * 
   * @param first The first potential value.
   * @param second The second potential value.
   * @param rest Any remaining potential values.
   * @return true if any of the potential values were not null.
   */
  @SafeVarargs // this class does not perform any operations on the "rest" array
  public final boolean asFirstOf(T first, T second, T... rest) {
    if (first != null) {
      this.val = first;
      return true;
    }
    else if (second != null) {
      this.val = second;
      return true;
    }
    else {
      return asFirstOf(rest);
    }
  }

  /**
   * Sets the variable to the first non-null value in the specified array.
   * 
   * @param vals An array of potential values for the variable.
   * @return true if the array was not null, and at least one of the potential values was not null.
   */
  public final boolean asFirstOf(T[] vals) {
    if (vals != null) {
      for (T val : vals) {
        if (val != null) {
          this.val = val;
          return true;
        }
      }
    }
    // no non-null values were found
    this.val = null;
    return false;
  }

  /**
   * Sets the variable to the value retrieved from the specified supplier.
   * 
   * @param supplier an {@link Supplier} able to return an instance of <T>.
   * @return true if the supplier was not null and returned a non-null value, otherwise false.
   */
  public final boolean from(Supplier<? extends T> supplier) {
    if (supplier == null) {
      this.val = null;
      return false;
    }
    else {
      return this.as(supplier.get());
    }
  }
  
  /**
   * Sets the variable to the first non-null value reached within the specified {@link Iterable}.
   * 
   * @param vals An {@link Iterable} of potential values for the variable.
   * @return true if the parameter was not null, and at least one of its values was not null.
   */
  public final boolean asFirstOf(Iterable<? extends T> vals) {
    if (vals != null) {
      for (T val : vals) {
        if (val != null) {
          this.val = val;
          return true;
        }
      }
    }
    // no non-null values were found
    this.val = null;
    return false;
  }

  /**
   * Retrieve the current (nullable) value of the variable, or null if it has not been defined.
   * 
   * @return the current (nullable) value of the variable, or null if it has not been defined.
   */
  @Override
  public final T get() {
    return this.val;
  }
  
  @Override
  public String toString() {
    return String.valueOf(this.val);
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    else if (!(obj instanceof Var<?>)) {
      return false;
    }
    else {
      Var<?> other = (Var<?>) obj;
      if (this.val == other.val) {
        return true;
      }
      else if (this.val == null) {
        return false;
      }
      else {
        return this.val.equals(other.get());
      }
    }
  }
  
  @Override
  public int hashCode() {
    T val = this.val; // avoids thread issues :(
    if (val == null) {
      return 0;
    }
    else {
      return val.hashCode();
    }
  }
}