package bloop.cli

import java.io.{InputStream, PrintStream}
import java.nio.file.{Path, Paths}

import bloop.cli.CommonOptions.PrettyProperties
import caseapp.CommandParser
import caseapp.core.{ArgParser, DefaultBaseCommand, Parser}
import shapeless.{Coproduct, Strict}

import scala.util.Try

object CliParsers {
  import caseapp.util.Implicit
  import caseapp.core.{Default, HListParser}
  import shapeless.{:+:, ::, HNil, CNil}
  import caseapp.core.Defaults

  implicit val caseappImplicitHnil: Implicit[HNil] = Implicit.hnil
  implicit val caseappImplicitNone: Implicit[None.type] = Implicit.instance(None)
  implicit val caseappImplicitNoneHnil: Implicit[None.type :+: CNil] =
    Implicit.instance(Coproduct(None))
  implicit val hnil: HListParser.Aux[HNil, HNil, HNil, HNil, HNil, HNil, HNil, HNil] = {
    HListParser.instance { (_, _, _, _, _) =>
      new caseapp.Parser[HNil] {
        type D = HNil
        def init = HNil
        def step(args: Seq[String], d: HNil) = Right(None)
        def get(d: HNil) = Right(HNil)
        def args = Vector.empty
      }
    }
  }

  implicit val caseappCorePrintStream: Default[PrintStream] =
    Default.instance[PrintStream](System.out)
  implicit val implicitCaseappCorePrintStream: Implicit[Default[PrintStream]] =
    Implicit.instance(Default.instance[PrintStream](System.out))
  implicit val optionDefaultPrintStream: Option[Default[PrintStream]] =
    Some(caseappCorePrintStream)
  implicit val someDefaultPrintStream: Some[Default[PrintStream]] =
    Some(caseappCorePrintStream)
  implicit val implicitOptionCaseappCorePrintStream: Implicit[Option[Default[PrintStream]]] =
    Implicit.instance(optionDefaultPrintStream)
  implicit val strictimplicitOptionCaseappCorePrintStream
    : Strict[Implicit[Option[Default[PrintStream]]]] =
    Strict.apply(Implicit.instance(optionDefaultPrintStream))
  implicit val implicitSomeCaseappCorePrintStream: Implicit[Some[Default[PrintStream]]] =
    Implicit.instance(someDefaultPrintStream)
  implicit val caseappCorePrintStreamHNil: Default[PrintStream] :: HNil =
    caseappCorePrintStream :: HNil

  implicit val caseappCorePath: Default[Path] =
    Default.instance[Path](CommonOptions.default.workingPath.underlying)
  implicit val implicitCaseappCorePath: Implicit[Default[Path]] =
    Implicit.instance(caseappCorePath)
  implicit val optionDefaultPath: Option[Default[Path]] =
    Some(caseappCorePath)
  implicit val someDefaultPath: Some[Default[Path]] =
    Some(caseappCorePath)
  implicit val implicitOptionCaseappCorePath: Implicit[Option[Default[Path]]] =
    Implicit.instance(optionDefaultPath)
  implicit val implicitSomeCaseappCorePath: Implicit[Some[Default[Path]]] =
    Implicit.instance(someDefaultPath)
  implicit val caseappCorePathHNil: Default[Path] :: HNil =
    caseappCorePath :: HNil

  implicit val caseappCoreInputStream: Default[InputStream] =
    Default.instance[InputStream](CommonOptions.default.in)
  implicit val implicitCaseappCoreInputStream: Implicit[Default[InputStream]] =
    Implicit.instance(caseappCoreInputStream)
  implicit val optionDefaultInputStream: Option[Default[InputStream]] =
    Some(caseappCoreInputStream)
  implicit val someDefaultInputStream: Some[Default[InputStream]] =
    Some(caseappCoreInputStream)
  implicit val implicitOptionCaseappCoreInputStream: Implicit[Option[Default[InputStream]]] =
    Implicit.instance(optionDefaultInputStream)
  implicit val implicitSomeCaseappCoreInputStream: Implicit[Some[Default[InputStream]]] =
    Implicit.instance(someDefaultInputStream)
  implicit val caseappCoreInputStreamHNil: Default[InputStream] :: HNil =
    caseappCoreInputStream :: HNil
  implicit val implicitSomeDefaultInputStreamNoneCnil
    : Implicit[Some[Default[InputStream]] :+: None.type :+: CNil] =
    Implicit.instance(Coproduct(someDefaultInputStream))

  implicit val caseappCorePrettyProperties: Default[PrettyProperties] =
    Default.instance[PrettyProperties](CommonOptions.default.env)
  implicit val implicitCaseappCorePrettyProperties: Implicit[Default[PrettyProperties]] =
    Implicit.instance(caseappCorePrettyProperties)
  implicit val optionDefaultPrettyProperties: Option[Default[PrettyProperties]] =
    Some(caseappCorePrettyProperties)
  implicit val someDefaultPrettyProperties: Some[Default[PrettyProperties]] =
    Some(caseappCorePrettyProperties)
  implicit val implicitOptionCaseappCorePrettyProperties
    : Implicit[Option[Default[PrettyProperties]]] =
    Implicit.instance(optionDefaultPrettyProperties)
  implicit val implicitSomeCaseappCorePrettyProperties: Implicit[Some[Default[PrettyProperties]]] =
    Implicit.instance(someDefaultPrettyProperties)
  implicit val caseappCorePrettyPropertiesHNil: Default[PrettyProperties] :: HNil =
    caseappCorePrettyProperties :: HNil
  implicit val implicitSomeDefaultPrettyPropertiesNoneCnil
    : Implicit[Some[Default[PrettyProperties]] :+: None.type :+: CNil] =
    Implicit.instance(Coproduct(someDefaultPrettyProperties))

  implicit val booleanReadStrict: Strict[ArgParser[Boolean]] = Strict(ArgParser.boolean)
  implicit val caseappCoreBooleanHnil: Default[Boolean] :: shapeless.HNil =
    Defaults.boolean :: shapeless.HNil
  implicit val implicitCaseappCoreBooleanHnil: Implicit[Default[Boolean] :: shapeless.HNil] =
    Implicit.instance(caseappCoreBooleanHnil)
  implicit val implicitSomeDefaultBooleanNoneCnil
    : Implicit[Some[Default[Boolean]] :+: None.type :+: CNil] =
    Implicit.instance(Coproduct(Some(Defaults.boolean)))

  implicit val strictArgParserString: Strict[ArgParser[String]] = Strict(ArgParser.string)
  implicit val defaultStringHnil: Default[String] :: shapeless.HNil =
    Defaults.string :: shapeless.HNil
  implicit val implicitStringHnil: Implicit[Default[String] :: shapeless.HNil] =
    Implicit.instance(defaultStringHnil)
  implicit val implicitSomeDefaultStringNoneCnil
    : Implicit[Some[Default[String]] :+: None.type :+: CNil] =
    Implicit.instance(Coproduct(Some(Defaults.string)))

  implicit val strictArgParserInt: Strict[ArgParser[Int]] = Strict(ArgParser.int)
  implicit val defaultIntHnil: Default[Int] :: shapeless.HNil = Defaults.int :: shapeless.HNil
  implicit val implicitIntHnil: Implicit[Default[Int] :: shapeless.HNil] =
    Implicit.instance(defaultIntHnil)
  implicit val implicitSomeDefaultIntNoneCnil: Implicit[Some[Default[Int]] :+: None.type :+: CNil] =
    Implicit.instance(Coproduct(Some(Defaults.int)))

  implicit val inputStreamRead: ArgParser[InputStream] =
    ArgParser.instance[InputStream]("stdin")(_ => Right(System.in))
  implicit val inputStreamReadStrict: Strict[ArgParser[InputStream]] =
    Strict(inputStreamRead)
  implicit val printStreamRead: ArgParser[PrintStream] =
    ArgParser.instance[PrintStream]("stdout")(_ => Right(System.out))
  implicit val printStreamReadStrict: Strict[ArgParser[PrintStream]] =
    Strict(printStreamRead)

  implicit val pathParser: ArgParser[Path] = ArgParser.instance("A filepath parser") {
    case supposedPath: String =>
      val toPath = Try(Paths.get(supposedPath)).toEither
      toPath.left.map(t => s"The provided path ${supposedPath} is not valid: '${t.getMessage()}'.")
  }

  implicit val completionFormatReadParser: ArgParser[completion.Format] = {
    ArgParser.instance[completion.Format]("format") {
      case "bash" => Right(completion.BashFormat)
      case "zsh" => Right(completion.ZshFormat)
      case w00t => Left(s"Unrecognized format: $w00t")
    }
  }
  implicit val strictCopmletionFormatReadParser: Strict[ArgParser[completion.Format]] =
    Strict(completionFormatReadParser)

  implicit val propertiesParser: ArgParser[PrettyProperties] = {
    ArgParser.instance("A properties parser") {
      case whatever => Left("You cannot pass in properties through the command line.")
    }
  }
  implicit val strictPropertiesParser: Strict[ArgParser[PrettyProperties]] =
    Strict(propertiesParser)

  implicit val CommonOptionsParser: Parser.Aux[CommonOptions, CommonOptions] =
    Parser.apply[CommonOptions].asInstanceOf[Parser.Aux[CommonOptions, CommonOptions]]
  implicit val OptionsParser: Parser.Aux[CliOptions, CliOptions] =
    Parser.apply[CliOptions].asInstanceOf[Parser.Aux[CliOptions, CliOptions]]

  val BaseMessages: caseapp.core.Messages[DefaultBaseCommand] =
    caseapp.core.Messages[DefaultBaseCommand]

  val CommandsMessages: caseapp.core.CommandsMessages[Commands.RawCommand] =
    implicitly[caseapp.core.CommandsMessages[Commands.RawCommand]]
  val CommandsParser: CommandParser[Commands.RawCommand] =
    implicitly[caseapp.core.CommandParser[Commands.RawCommand]]
}
