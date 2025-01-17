package com.tencent.angel.mlcore.local.optimizer

import java.util.concurrent.Future

import com.tencent.angel.ml.math2.ufuncs.Ufuncs
import com.tencent.angel.mlcore.conf.{MLCoreConf, SharedConf}
import com.tencent.angel.mlcore.local.LocalOptimizerProvider
import com.tencent.angel.mlcore.local.variables.{LocalBlasMatVariable, LocalMatVariable, LocalVecVariable}
import com.tencent.angel.mlcore.optimizer.{Optimizer, OptimizerProvider}
import com.tencent.angel.mlcore.utils.{OptUtils, OptimizerKeys}
import com.tencent.angel.mlcore.variable.Variable
import org.json4s.JsonAST.{JField, JObject, JString}


private[mlcore] class SGD(override var lr: Double) extends Optimizer {
  override val numSlot: Int = 1

  override def update[T](variable: Variable, epoch: Int, batchSize: Int): Future[T] = {
    variable match {
      case v: LocalBlasMatVariable =>
        val value = v.storage.getRow(0)
        val grad = if (regL2Param == 0) {
          v.storage.getRow(1)
        } else {
          value.axpy(v.storage.getRow(1), regL2Param)
        }

        value.isub(grad.imul(lr))
        if (regL1Param != 0.0) {
          Ufuncs.isoftthreshold(value, regL1Param)
        }

        grad.imul(0.0)
      case v: LocalMatVariable =>
        val numFactors: Int = v.numRows
        val value = OptUtils.getRowsAsMatrix(v.storage, 0, numFactors)
        val grad = if (regL2Param == 0) {
          OptUtils.getRowsAsMatrix(v.storage, numFactors, numFactors * 2)
        } else {
          value.axpy(OptUtils.getRowsAsMatrix(v.storage, numFactors, numFactors * 2), regL2Param)
        }

        value.isub(grad.imul(lr))
        if (regL1Param != 0.0) {
          Ufuncs.isoftthreshold(value, regL1Param)
        }

        grad.imul(0.0)
      case v: LocalVecVariable =>
        val value = v.storage.getRow(0)
        val grad = if (regL2Param == 0) {
          v.storage.getRow(1)
        } else {
          value.axpy(v.storage.getRow(1), regL2Param)
        }

        value.isub(grad.imul(lr))
        if (regL1Param != 0.0) {
          Ufuncs.isoftthreshold(value, regL1Param)
        }

        grad.imul(0.0)
    }

    null.asInstanceOf[Future[T]]
  }

  private[mlcore] override def toJson: JObject = {
    JObject(JField(OptimizerKeys.typeKey, JString(s"${this.getClass.getSimpleName}")))
  }
}


private[mlcore] object SGD {
  private[mlcore] def fromJson(jast: JObject, provider: OptimizerProvider)(implicit conf: SharedConf): SGD = {
    val laProvider = provider.asInstanceOf[LocalOptimizerProvider]
    assert(laProvider.fieldEqualClassName[SGD](jast, OptimizerKeys.typeKey))

    val regL1Param: Double = conf.getDouble(MLCoreConf.ML_REG_L1, MLCoreConf.DEFAULT_ML_REG_L1)
    val regL2Param: Double = conf.getDouble(MLCoreConf.ML_REG_L2, MLCoreConf.DEFAULT_ML_REG_L2)
    val lr = conf.getDouble(MLCoreConf.ML_LEARN_RATE, MLCoreConf.DEFAULT_ML_LEARN_RATE)
    val opt = new SGD(lr)
    opt.setRegL1Param(regL1Param).setRegL2Param(regL2Param)
  }
}