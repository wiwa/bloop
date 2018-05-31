class Foo { type R }

object Foo {
  type Aux[R2] = Foo { type R = R2 }
  implicit val foo: Foo.Aux[_] = new Foo {
    type R = Int
  }
}

object Main {
  type HD >: Nothing <: Any
  implicitly[Foo.Aux[_ >: Nothing <: Any]]
}