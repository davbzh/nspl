package org.nspl

import org.nspl.data._
import scala.collection.JavaConversions._
import org.saddle._

object saddle {

  def rasterplotFromFrame[RX, CX](
    dataFrame: Frame[RX, CX, Double],
    main: String = "",
    xlab: String = "",
    ylab: String = "",
    xLabFontSize: Option[RelFontSize] = None,
    yLabFontSize: Option[RelFontSize] = None,
    mainFontSize: RelFontSize = 1 fts,
    colormap: Colormap = HeatMapColors(0, 1),
    xWidth: RelFontSize = 20 fts,
    yHeight: RelFontSize = 20 fts,
    valueText: Boolean = false,
    valueColor: Color = Color.black,
    valueFontSize: RelFontSize = 0.4 fts
  ) =
    rasterplot(
      asRaster(dataFrame.toMat),
      main,
      xlab,
      ylab,
      xLabFontSize.getOrElse(math.min(2.0, xWidth.v / dataFrame.numCols) fts),
      yLabFontSize.getOrElse(math.min(2.0, yHeight.v / dataFrame.numRows) fts),
      mainFontSize,
      colormap,
      dataFrame.colIx.toSeq.map(_.toString).zipWithIndex.map(x => x._2.toDouble + 0.5 -> x._1),
      dataFrame.rowIx.toSeq.map(_.toString).zipWithIndex.map(x => x._2.toDouble + 0.5 -> x._1),
      xWidth = xWidth,
      yHeight = yHeight,
      valueText = valueText,
      valueColor = valueColor,
      valueFontSize = valueFontSize
    )

  def asRaster(mat: Mat[Double]): DataMatrix =
    new DataMatrix(mat.contents, mat.numCols, mat.numRows)

  implicit def dataSourceFromMat(mat: Mat[Double]): DataTable =
    new DataTable(mat.contents, mat.numCols)

  implicit def dataSourceFrom1DVec(vec: Vec[Double]): DataSourceWithQuantiles =
    indexed(vec.toSeq)

  implicit def dataSourceFromSeries[R](s: Series[R, Double]): DataSourceWithQuantiles =
    new DataSourceWithQuantiles {
      def iterator =
        s.toSeq.iterator.zipWithIndex.map(x => VectorRow(Vector(x._2, x._1._2), x._1._1.toString))
      def dimension = 2
      def columnMinMax(i: Int) = i match {
        case 0 => MinMaxImpl(0, s.length - 1d)
        case 1 => MinMaxImpl(s.min.get, s.max.get)
      }
      def quantilesOfColumn(i: Int, qs: Vector[Double]) = {
        assert(i == 1 || i == 0)
        val v =
          if (i == 1) s.toVec.toSeq.sorted
          else (0 until s.length).map(_.toDouble)
        percentile(v, qs).toVector
      }
    }

  def dataSourceFromRowMajorVec(vec: Vec[Double], numCols: Int): DataTable =
    new DataTable(vec, numCols)

  implicit def dataSourceFromZippedVecs2(
    vec1: (Vec[Double], Vec[Double])
  ): DataSourceWithQuantiles =
    dataSourceFromFrame(Frame(vec1._1, vec1._2))

  implicit def dataSourceFromZippedVecs3(
    vec1: (Vec[Double], Vec[Double], Vec[Double])
  ): DataSourceWithQuantiles =
    dataSourceFromFrame(Frame(vec1._1, vec1._2, vec1._3))

  implicit def dataSourceFromZippedVecs4(
    vec1: (Vec[Double], Vec[Double], Vec[Double], Vec[Double])
  ): DataSourceWithQuantiles =
    dataSourceFromFrame(Frame(vec1._1, vec1._2, vec1._3, vec1._4))

  implicit def dataSourceFromFrame[RX, CX](
    frame: Frame[RX, CX, Double]
  ): DataSourceWithQuantiles =
    new DataSourceWithQuantiles {

      def iterator =
        frame.toRowSeq.map {
          case (rx, series) =>
            VectorRow(series.toVec.toSeq.toVector, rx.toString)
        }.iterator

      def dimension = frame.numCols

      def columnMinMax(i: Int): MinMax = {
        val v = frame.colAt(i).toVec
        MinMaxImpl(v.min.get, v.max.get)
      }

      def quantilesOfColumn(i: Int, qs: Vector[Double]) = {
        val v = frame.colAt(i).toVec
        percentile(v.toSeq, qs).toVector

      }
    }
}
