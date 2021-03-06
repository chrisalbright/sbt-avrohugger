package sbtavrohugger

import avrohugger.Generator
import avrohugger.format.{Scavro, SpecificRecord, Standard}
import avrohugger.types.AvroScalaTypes
import java.io.File

import sbt.Keys._
import sbt._

/**
 * Simple plugin for generating the Scala sources for Avro IDL, schemas and protocols.
 */
object SbtAvrohugger extends AutoPlugin {
  
  object autoImport {
    
    // sbt tasks:
    lazy val avroScalaGenerateScavro   = taskKey[Seq[File]]("Generate Scala sources for Scavro")
    lazy val avroScalaGenerateSpecific = taskKey[Seq[File]]("Generate Scala sources implementing SpecificRecord")
    lazy val avroScalaGenerate         = taskKey[Seq[File]]("Generate Scala sources from avro files")

    // sbt settings
      // Scavro Format
    lazy val avroScavroSourceDirectory      = settingKey[File]("Avro schema directory for generating Scavro Scala")
    lazy val avroScavroScalaSource          = settingKey[File]("Scavro Scala source directory for compiled avro")
    lazy val avroScalaScavroCustomTypes     = settingKey[AvroScalaTypes]("Customize Avro to Scala type map by type")
    lazy val avroScalaScavroCustomNamespace = settingKey[Map[String, String]]("Custom namespace of generated Scavro Scala code")
      // SpecificRecord Format
    lazy val avroSpecificSourceDirectory      = settingKey[File]("Avro schema directory for generating SpecificRecord")
    lazy val avroSpecificScalaSource          = settingKey[File]("Specific Scala source directory for compiled avro")
    lazy val avroScalaSpecificCustomTypes     = settingKey[AvroScalaTypes]("Custom Avro to Scala type map")
    lazy val avroScalaSpecificCustomNamespace = settingKey[Map[String, String]]("Custom namespace of generated Specific Scala code")
      // Standard Format
    lazy val avroSourceDirectory      = settingKey[File]("Avro schema directory for Scala code generation")
    lazy val avroScalaSource          = settingKey[File]("Scala source directory for compiled avro")
    lazy val avroScalaCustomTypes     = settingKey[AvroScalaTypes]("Custom Scala types of generated Scala code")
    lazy val avroScalaCustomNamespace = settingKey[Map[String, String]]("Custom namespace of generated Scala code")
  }
    
  import autoImport._
  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements
  
  lazy val baseSettings = 
    avroSettings ++
    scavroSettings ++
    specificAvroSettings
    
  override lazy val projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(baseSettings) ++
    inConfig(Test)(baseSettings) 
    
  // Standard Format
  lazy val avroSettings: Seq[Def.Setting[_]] = Seq(
    avroScalaSource               := sourceManaged.value / "compiled_avro",
    avroSourceDirectory           := sourceDirectory.value / "avro",
    avroScalaCustomNamespace      := Map.empty[String, String],
    avroScalaCustomTypes          := Standard.defaultTypes,
    logLevel in avroScalaGenerate := (logLevel?? Level.Info).value,
    avroScalaGenerate := {
      val cache = target.value
      val srcDir = avroSourceDirectory.value
      val targetDir = avroScalaSource.value
      val out = streams.value
      val scalaV = scalaVersion.value
      val customTypes = avroScalaCustomTypes.value
      val customNamespace = avroScalaCustomNamespace.value
      val cachedCompile = FileFunction.cached(cache / "avro",
        inStyle = FilesInfo.lastModified,
        outStyle = FilesInfo.exists) { (in: Set[File]) =>
          val isNumberOfFieldsRestricted = scalaV == "2.10"
          val gen = new Generator(
            format = Standard,
            avroScalaCustomTypes = Some(customTypes),
            avroScalaCustomNamespace = customNamespace,
            restrictedFieldNumber = isNumberOfFieldsRestricted)
          FileWriter.generateCaseClasses(gen, srcDir, targetDir, out.log)
        }
      cachedCompile((srcDir ** "*.av*").get.toSet).toSeq
    }
  )
  
  // Scavro Format
  lazy val scavroSettings: Seq[Def.Setting[_]] = Seq(
    avroScavroScalaSource          := sourceManaged.value / "compiled_avro",
    avroScavroSourceDirectory      := sourceDirectory.value / "avro",
    avroScalaScavroCustomTypes     := Scavro.defaultTypes,
    avroScalaScavroCustomNamespace := Map.empty[String, String],
    logLevel in avroScalaGenerateScavro  := (logLevel?? Level.Info).value,
    avroScalaGenerateScavro := {
      val cache = target.value
      val srcDir = avroScavroSourceDirectory.value
      val targetDir = avroScavroScalaSource.value
      val out = streams.value
      val scalaV = scalaVersion.value
      val scavroCustomTypes = avroScalaScavroCustomTypes.value
      val scavroCustomNamespace = avroScalaScavroCustomNamespace.value
      val cachedCompile = FileFunction.cached(cache / "avro",
        inStyle = FilesInfo.lastModified,
        outStyle = FilesInfo.exists) { (in: Set[File]) =>
          val isNumberOfFieldsRestricted = scalaV == "2.10"
          val gen = new Generator(
            Scavro,
            Some(scavroCustomTypes),
            scavroCustomNamespace,
            isNumberOfFieldsRestricted)
          FileWriter.generateCaseClasses(gen, srcDir, targetDir, out.log)
        }
      cachedCompile((srcDir ** "*.av*").get.toSet).toSeq
    }
  )
  
  // SpecificRecord Format
  lazy val specificAvroSettings: Seq[Def.Setting[_]] = Seq(
    avroSpecificScalaSource := sourceManaged.value / "compiled_avro",
    avroSpecificSourceDirectory := sourceDirectory.value / "avro",
    avroScalaSpecificCustomTypes := SpecificRecord.defaultTypes,
    avroScalaSpecificCustomNamespace := Map.empty[String, String],
    logLevel in avroScalaGenerateSpecific := (logLevel?? Level.Info).value,
    avroScalaGenerateSpecific := {
      val cache = target.value
      val srcDir = avroSpecificSourceDirectory.value
      val targetDir = avroSpecificScalaSource.value
      val out = streams.value
      val scalaV = scalaVersion.value
      val specificCustomTypes = avroScalaSpecificCustomTypes.value
      val specificCustomNamespace = avroScalaSpecificCustomNamespace.value
      val cachedCompile = FileFunction.cached(cache / "avro",
        inStyle = FilesInfo.lastModified,
        outStyle = FilesInfo.exists) { (in: Set[File]) =>
          val isNumberOfFieldsRestricted = scalaV == "2.10"
          val gen = new Generator(
            SpecificRecord,
            Some(specificCustomTypes),
            specificCustomNamespace,
            isNumberOfFieldsRestricted)
          FileWriter.generateCaseClasses(gen, srcDir, targetDir, out.log)
        }
      cachedCompile((srcDir ** "*.av*").get.toSet).toSeq
    }
  )
}
