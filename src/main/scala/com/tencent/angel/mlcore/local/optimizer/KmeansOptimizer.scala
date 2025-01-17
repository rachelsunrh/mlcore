package com.tencent.angel.mlcore.local.optimizer

import java.util.concurrent.Future

import com.tencent.angel.mlcore.local.variables.{LocalBlasMatVariable, LocalMatVariable, LocalVecVariable}
import com.tencent.angel.mlcore.optimizer.Optimizer
import com.tencent.angel.mlcore.utils.{JsonUtils, OptUtils, OptimizerKeys}
import com.tencent.angel.mlcore.variable.Variable
import org.json4s.JsonAST
import org.json4s.JsonAST.{JField, JObject, JString}

private [mlcore] class KmeansOptimizer extends Optimizer {
  override var lr: Double = _

  private[mlcore] override def toJson: JsonAST.JObject = {
    JObject(JField(OptimizerKeys.typeKey, JString(s"${this.getClass.getSimpleName}")))
  }

  override val numSlot: Int = 1

  override def update[T](variable: Variable, epoch: Int, batchSize: Int): Future[T] = {
    variable match {
      case v: LocalBlasMatVariable =>
        val value = v.storage.getRow(0)
        val grad = v.storage.getRow(1)
        value.iadd(grad)
        grad.imul(0.0)
      case v: LocalMatVariable =>
        val numFactors: Int = v.numRows
        val value = OptUtils.getRowsAsMatrix(v.storage, 0, numFactors)
        val grad = OptUtils.getRowsAsMatrix(v.storage, numFactors, numFactors * 2)
        value.iadd(grad)
        grad.imul(0.0)
      case v: LocalVecVariable =>
        val value = v.storage.getRow(0)
        val grad = v.storage.getRow(1)
        value.iadd(grad)
        grad.imul(0.0)
    }

    null.asInstanceOf[Future[T]]
  }
}


private [mlcore] object KmeansOptimizer {

  private[mlcore] def fromJson(jast: JObject): KmeansOptimizer = {
    assert(JsonUtils.fieldEqualClassName[KmeansOptimizer](jast, OptimizerKeys.typeKey))
    new KmeansOptimizer()
  }
} 