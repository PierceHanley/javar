package org.ph0.javar;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

public class VarTest {
  @Test
  public void testVarDef_NonNull() {
    Var<String> var = Var.var();
    String realValue = new String("Hello");
    String val = var.def(var.as(realValue) || var.as("World"));
    assertThat(realValue, sameInstance(val));
    assertThat(realValue, sameInstance(var.get()));
  }
  
  @Test
  public void testVarDef_FirstValueNull() {
    Var<String> var = Var.var();
    String realValue = "I'm not null!";
    String val = var.def(var.as(null) || var.as(realValue));
    assertThat(realValue, sameInstance(val));
    assertThat(realValue, sameInstance(var.get()));
  }
  
  @Test
  public void testVarDef_ShortCircuit() {
    final int initialValue = 7; // 0 is problematic because it's hard to distinguish from defaults
    final AtomicInteger counter = new AtomicInteger(initialValue);
    class Widget { int id = counter.getAndIncrement(); }

    assertThat(initialValue, equalTo(counter.get()));
    Var<Widget> var = Var.var();
    assertThat(initialValue, equalTo(counter.get()));
    Widget w = var.def(var.as(new Widget()) || var.as(new Widget()));
    
    // only one Widget should've been created above, so the counter should be one greater than the
    // initial value and the ID of the variable's value should be the initial one.
    assertThat(w.id, equalTo(initialValue));
    assertThat(counter.get(), equalTo(initialValue + 1));
  }
}