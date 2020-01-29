package myapp.plugins

import distage.plugins.PluginDef
import myapp.MyRole

object MyappPlugin extends PluginDef {
  make[MyRole]
}
