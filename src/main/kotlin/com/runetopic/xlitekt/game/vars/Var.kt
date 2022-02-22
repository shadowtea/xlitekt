package com.runetopic.xlitekt.game.vars

import com.runetopic.xlitekt.shared.resource.VarInfoResource

abstract class Var(
    open val name: String,
) {
    abstract val info: VarInfoResource
    abstract val varType: VarType
}
