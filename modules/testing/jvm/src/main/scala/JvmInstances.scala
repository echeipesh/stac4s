package com.azavea.stac4s.testing

import com.azavea.stac4s.extensions.layer.StacLayer
import com.azavea.stac4s.extensions.periodic.PeriodicExtent
import com.azavea.stac4s.syntax._
import com.azavea.stac4s.types.TemporalExtent
import com.azavea.stac4s.{
  Bbox,
  Interval,
  ItemCollection,
  SpatialExtent,
  StacCollection,
  StacExtent,
  StacItem,
  StacItemAsset,
  StacLink,
  StacVersion
}

import cats.syntax.apply._
import cats.syntax.functor._
import geotrellis.vector.{Geometry, Point, Polygon}
import io.circe.JsonObject
import io.circe.syntax._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.cats.implicits._
import org.scalacheck.{Arbitrary, Gen}
import org.threeten.extra.PeriodDuration

import java.time.{Duration, Instant, Period}

trait JvmInstances {

  private[testing] def temporalExtentGen: Gen[TemporalExtent] = {
    (arbitrary[Instant], arbitrary[Instant]).tupled
      .map { case (start, end) =>
        TemporalExtent(start, end)
      }
  }

  private[testing] def intervalGen: Gen[Interval] =
    (periodicExtentGen, temporalExtentGen).tupled flatMap { case (periodicity, temporalExtent) =>
      Gen.oneOf(
        Gen.const(Interval(List(temporalExtent))),
        Gen.const(Interval(List(temporalExtent)).addExtensionFields(periodicity))
      )
    }

  private[testing] def rectangleGen: Gen[Geometry] =
    (for {
      lowerX <- Gen.choose(0.0, 1000.0)
      lowerY <- Gen.choose(0.0, 1000.0)
    } yield {
      Polygon(
        Point(lowerX, lowerY),
        Point(lowerX + 100, lowerY),
        Point(lowerX + 100, lowerY + 100),
        Point(lowerX, lowerY + 100),
        Point(lowerX, lowerY)
      )
    }).widen

  private[testing] def stacItemGen: Gen[StacItem] =
    (
      nonEmptyStringGen,
      Gen.const("0.8.0"),
      Gen.const(List.empty[String]),
      Gen.const("Feature"),
      rectangleGen,
      TestInstances.twoDimBboxGen,
      nonEmptyListGen(TestInstances.stacLinkGen) map { _.toList },
      Gen.nonEmptyMap((nonEmptyStringGen, TestInstances.cogAssetGen).tupled),
      Gen.option(nonEmptyStringGen),
      TestInstances.itemExtensionFieldsGen
    ).mapN(StacItem.apply)

  private[testing] def stacItemShortGen: Gen[StacItem] =
    (
      nonEmptyStringGen,
      Gen.const("0.8.0"),
      Gen.const(List.empty[String]),
      Gen.const("Feature"),
      rectangleGen,
      TestInstances.twoDimBboxGen,
      Gen.const(Nil),
      Gen.const(Map.empty[String, StacItemAsset]),
      Gen.option(nonEmptyStringGen),
      TestInstances.itemExtensionFieldsGen
    ).mapN(StacItem.apply)

  private[testing] def itemCollectionGen: Gen[ItemCollection] =
    (
      Gen.const("FeatureCollection"),
      Gen.const(StacVersion.unsafeFrom("1.0.0-beta.2")),
      Gen.const(Nil),
      Gen.listOf[StacItem](stacItemGen),
      Gen.listOf[StacLink](TestInstances.stacLinkGen),
      Gen.const(().asJsonObject)
    ).mapN(ItemCollection.apply)

  private[testing] def itemCollectionShortGen: Gen[ItemCollection] =
    (
      Gen.const("FeatureCollection"),
      Gen.const(StacVersion.unsafeFrom("1.0.0-beta.2")),
      Gen.const(Nil),
      Gen.listOf[StacItem](stacItemGen),
      Gen.const(Nil),
      Gen.const(().asJsonObject)
    ).mapN(ItemCollection.apply)

  private[testing] def stacExtentGen: Gen[StacExtent] =
    (
      TestInstances.bboxGen,
      temporalExtentGen
    ).mapN((bbox: Bbox, interval: TemporalExtent) => StacExtent(SpatialExtent(List(bbox)), Interval(List(interval))))

  private[testing] def stacCollectionGen: Gen[StacCollection] =
    (
      nonEmptyStringGen,
      possiblyEmptyListGen(nonEmptyStringGen),
      nonEmptyStringGen,
      Gen.option(nonEmptyStringGen),
      nonEmptyStringGen,
      possiblyEmptyListGen(nonEmptyStringGen),
      TestInstances.stacLicenseGen,
      possiblyEmptyListGen(TestInstances.stacProviderGen),
      stacExtentGen,
      Gen.const(().asJsonObject),
      Gen.const(JsonObject.fromMap(Map.empty)),
      possiblyEmptyListGen(TestInstances.stacLinkGen),
      TestInstances.collectionExtensionFieldsGen
    ).mapN(StacCollection.apply)

  private[testing] def stacCollectionShortGen: Gen[StacCollection] =
    (
      nonEmptyStringGen,
      possiblyEmptyListGen(nonEmptyStringGen),
      nonEmptyStringGen,
      Gen.option(nonEmptyStringGen),
      nonEmptyStringGen,
      Gen.const(Nil),
      TestInstances.stacLicenseGen,
      Gen.const(Nil),
      stacExtentGen,
      Gen.const(().asJsonObject),
      Gen.const(JsonObject.fromMap(Map.empty)),
      Gen.const(Nil),
      Gen.const(().asJsonObject)
    ).mapN(StacCollection.apply)

  private[testing] def stacLayerGen: Gen[StacLayer] = (
    nonEmptyAlphaRefinedStringGen,
    TestInstances.bboxGen,
    rectangleGen,
    TestInstances.stacLayerPropertiesGen,
    Gen.listOfN(8, TestInstances.stacLinkGen),
    Gen.const("Feature")
  ).mapN(
    StacLayer.apply
  )

  private[testing] def periodDurationGen: Gen[PeriodDuration] = for {
    years  <- Gen.choose(0, 100)
    months <- Gen.choose(0, 12)
    days   <- Gen.choose(0, 100)
    // minutes is an Int because with a Long we overflow during the test.
    // since Long.MaxValue is a suuuuuuuper unlikely quantity of minutes in a
    // duration, I'm declaring this More or Less Fine™
    minutes <- arbitrary[Int]
  } yield PeriodDuration.of(
    Period.of(years, months, days),
    Duration.ofMinutes(minutes.toLong)
  )

  private[testing] def periodicExtentGen: Gen[PeriodicExtent] = (
    periodDurationGen
  ) map {
    PeriodicExtent.apply
  }

  implicit val arbItem: Arbitrary[StacItem] = Arbitrary { stacItemGen }

  val arbItemShort: Arbitrary[StacItem] = Arbitrary { stacItemShortGen }

  implicit val arbItemCollection: Arbitrary[ItemCollection] = Arbitrary {
    itemCollectionGen
  }

  val arbItemCollectionShort: Arbitrary[ItemCollection] = Arbitrary {
    itemCollectionShortGen
  }

  implicit val arbGeometry: Arbitrary[Geometry] = Arbitrary { rectangleGen }

  implicit val arbInstant: Arbitrary[Instant] = Arbitrary { instantGen }

  implicit val arbCollection: Arbitrary[StacCollection] = Arbitrary {
    stacCollectionGen
  }

  val arbCollectionShort: Arbitrary[StacCollection] = Arbitrary { stacCollectionShortGen }

  implicit val arbStacExtent: Arbitrary[StacExtent] = Arbitrary {
    stacExtentGen
  }

  implicit val arbTemporalExtent: Arbitrary[TemporalExtent] = Arbitrary {
    temporalExtentGen
  }

  implicit val arbStaclayer: Arbitrary[StacLayer] = Arbitrary {
    stacLayerGen
  }

  implicit val arbPeriodDuration: Arbitrary[PeriodDuration] = Arbitrary {
    periodDurationGen
  }

  implicit val arbPeriodicExtent: Arbitrary[PeriodicExtent] = Arbitrary {
    periodicExtentGen
  }

  implicit val arbInterval: Arbitrary[Interval] = Arbitrary {
    intervalGen
  }
}

object JvmInstances extends JvmInstances {}
