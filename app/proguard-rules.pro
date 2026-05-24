# ProGuard rules for 78ham-ardptt

# Keep attributes for Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep Gson TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep data classes used with Gson (network layer)
-keep class com.nrlptt.app.network.UserInfo { *; }
-keep class com.nrlptt.app.network.RoomInfo { *; }
-keep class com.nrlptt.app.network.GroupInfo { *; }
-keep class com.nrlptt.app.network.DeviceData { *; }
-keep class com.nrlptt.app.network.PlatformServer { *; }
-keep class com.nrlptt.app.network.ServerConnection$MessageEntry { *; }
-keep class com.nrlptt.app.network.ServerConnection$ActivityEntry { *; }

# Keep data classes used with Gson (data layer)
-keep class com.nrlptt.app.data.ServerConfig { *; }
-keep class com.nrlptt.app.data.UserSettings { *; }

# Keep service classes
-keep class com.nrlptt.app.service.PttService { *; }
-keep class com.nrlptt.app.service.BootReceiver { *; }
