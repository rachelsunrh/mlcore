package com.tencent.angel.mlcore.local

import com.tencent.angel.ml.math2.vector.Vector
import com.tencent.angel.mlcore.conf.SharedConf
import com.tencent.angel.mlcore.network.EnvContext
import com.tencent.angel.mlcore.variable.VariableManager
import org.apache.hadoop.conf.Configuration

import scala.collection.JavaConversions._

class LocalVariableManager(isSparseFormat: Boolean, conf: SharedConf)
  extends VariableManager(isSparseFormat, conf) {

  override def createALL[T](envCtx: EnvContext[T]): Unit = {
    variables.values().foreach { variable => variable.create(envCtx) }
  }

  override def loadALL[T](envCtx: EnvContext[T], path: String, conf: Configuration): Unit = {
    variables.values().foreach { variable => variable.load(envCtx, path, conf) }
  }

  override def pullALL(epoch: Int, indices: Vector = null): Unit = {
    // val isSparseFormat = graph.dataFormat == "libsvm" || graph.dataFormat == "dummy"

    variables.values().foreach {
      case variable if isSparseFormat && variable.allowPullWithIndex =>
        variable.pull(epoch, indices)
      case variable => variable.pull(epoch)
    }
  }

  override def pull(name: String, epoch: Int = 0, indices: Vector = null): Unit = {
    // val isSparseFormat = graph.dataFormat == "libsvm" || graph.dataFormat == "dummy"

    val variable = getVariable(name)
    if (variable != null) {
      variable match {
        case v if isSparseFormat && v.allowPullWithIndex =>
          v.pull(epoch, indices)
        case v => v.pull(epoch)
      }
    }

  }

  override def saveALL[T](envCtx: EnvContext[T], path: String): Unit = {
    variables.values().foreach { variable => variable.save(envCtx, path) }
  }
}

