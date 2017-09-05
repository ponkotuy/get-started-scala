# Get started Scala Playframework

---

## おまえだれだよ
- ぽんこつ(@ponkotuy)
- Itandiのエンジニア
- 最近個人で作ったPlayアプリ
  - MyFleetGirls: [myfleet.moe](https://myfleet.moe)
  - TrainStampRally: [train.ponkotuy.com](https://train.ponkotuy.com)

---

## 今日やること
Logを保存するWebアプリを作る

- Viewは作らない
- 典型的なREST API
- ORM使ってDBやる

初期セットアップ終わってますよね？

---

## 進め方

- 一気に説明
- 一気に実装
- 半分ぐらいできてそうなら次の説明

実装時は各自資料を手元で見てください

https://ponkotuy.github.io/get-started-scala/

質問も勿論OKですよ

---

### 仕様

- GET /log/:id でIDからログを1件取得
- GET /logs でログの一覧を取得
- POST /log でログを投稿
- DELETE /log/:id でログを削除

典型的なREST APIですね

---

## index.html表示

テストに使うindex.htmlを表示できるように

- index.htmlをダウンロード [goo.gl/iVaSMp](https://raw.githubusercontent.com/ponkotuy/get-started-scala/master/public/index.html)
- public/直下に↑を置く(既存のものは消してOK)
- app/controllers/HomeController.scalaのindexメソッドを書き換え

  ```Assets.versioned(path="/public", file="index.html")```

---

- conf/application.confでSecurityHeadersFilterを外す

  ```play.filters.disabled += "play.filters.headers.SecurityHeadersFilter"```

- 起動してlocalhost:9000にアクセス

---

## モデル作成

DBの1レコードに紐付けるcase classを作る

データ郡を作りたくなったら大体case class

app/models/Log.scala とかに以下を

```
package models

case class Log(id: Long, logType: LogType, content: String)
```

※LogTypeは次で作ります

new Log(1L, LogType.Exception, "Not found a") で作成

---

### LogType

所謂enum型をScalaで

```
sealed abstract class LogType(val value: Int)

object LogType {
  object NotFoundError extends LogType(1)
  object Exception extends LogType(2)
  object FormatError extends LogType(3)
}
```

Scala内では必ずLogTypeオブジェクトで処理

- sealedは同じファイル内でしか継承できない
- abstract classは抽象class(具象化newできない

---

## CRUDMapper

Logに対するDBの処理をobject Logに書いていく

```
import scalikejdbc._

object Log extends SkinnyCRUDMapperWithId[Long, Log] {
  override def idToRawValue(id: Long) = id
  override def rawValueToId(value: Any) = value.toString.toLong

  override def defaultAlias = createAlias("l")
  override def extract(rs: WrappedResultSet, n: ResultName[Log]) = autoConstruct(rs, n)
}
```

objectはシングルトンクラス

Scalaでは同じ名前のclassとobjectが併存できる

---

### TypeBinder

LogTypeをDBに保存するのと取り出すルールを設定する

object LogTypeに以下を追記する

```
val values = NotFoundError :: Exception :: FormatError :: Nil
def find(value: Int): Option[LogType] = values.find(_.value == value)

implicit val typeBinder: TypeBinder[LogType] = TypeBinder.int.map { i => find(i).get }
```

最後の1行でSkinnyでの変換方法を定義する

---

## DB設定

DB接続の設定を書く

DBはこちら側で用意したMySQLで

build.sbtへ必要なライブラリの追加

```
libraryDependencies ++= Seq(
  "org.skinny-framework" %% "skinny-orm" % "2.4.0",
  "org.scalikejdbc" %% "scalikejdbc-play-initializer" % "2.6.0",
  "mysql" % "mysql-connector-java" % "6.0.6"
)
```

---

### application.conf設定

以下のDB接続設定をコピペ

```
db.default {
  driver = com.mysql.cj.jdbc.Driver
  url = "jdbc:mysql://get-started-scala-cluster.cluster-cxeueqhdrdav.ap-northeast-1.rds.amazonaws.com:3306/play?useUnicode=true&characterEncoding=utf8"
  user = "scalachan"
  password = "scalachan"
}
```

---

### Controller

HomeControllerを参考にapp/controllers/LogController.scalaを作る

```
@Singleton
class LogController @Inject()(val cc: ControllerComponents) extends AbstractController(cc) {
  def show(id: Long) = Action {
    Log.findById(id).fold(NotFound(s"Not found id=${id}")) { log => Ok(log.asJson) }
  }
}
```

- @Injectと@SingletonはGuiceというDIコンテナ設定
- Log.findByIdの返り値はOption[Log]型
- Optionは失敗する可能性のある処理での返り値
- Scalaでnullは御法度。Optionを使う
- foldで成功時と失敗時の処理を書く
- asJsonは現状では動かない

---

## Serialize

取得したレコードをJSONにシリアライズする

Circe(キルケー)を使う

build.sbtに追記

```
libraryDependencies += "play-circe" %% "play-circe" % "2608.3"
```

---

### Controller with Circe

controllerでCirceを有効にする

- trait CirceをControllerにmixinする
- 以下をimportする

```
import io.circe.generic.auto._
import io.circe.syntax._
```

---

### LogTypeのSerialize設定

Circeはcase classと標準の型を自動でSerializeする

LogTypeのSerialize方法だけ分からない

LogType objectに以下を追加してヒントを与える

```
implicit val encoder: Encoder[LogType] = Encoder.forProduct2("name", "value") { u =>
  (u.toString, u.value)
}
```

---

### routes

URLとControllerを紐付けるconf/routesを設定する

```
GET /log/:id controllers.LogController.show(id: Long)
```

---

### 動作確認

以上をちゃんとやれば動くはず

以下で確認

http://localhost:9000/log/1

自前のDBでやる場合はtable作成の処理が必要

---

## GET /logs

LogController

```
def list() = Action {
  Ok(Log.findAll().asJson)
}
```

routes

```
GET /logs controllers.LogController.list()
```

---

## POST /log

JSONで作成するlogを受けとる

受け取るJSONをcase classで

例えばqueries/CreateLog.scala

```
case class CreateLog(logType: Int, content: String)
```

---

### Log.create

CreateLog classからDBにレコードを追加するコード

object Logに追加

```
def create(log: CreateLog)(implicit session: DBSession = autoSession): Long = {
  createWithAttributes(
    'logType -> log.logType,
    'content -> log.content
  )
}
```

DBSessionはTransactionなどでSession管理できる

返り値はauto_incrementで生成したID

---

### Controller

LogControllerに以下を追記

```
def create() = Action(circe.tolerantJson[CreateLog]) { req =>
  if(Log.create(req.body) > 0) Ok("Success") else InternalServerError("SQLError")
}
```

- circe.tolerantJson[CreateLog]でbodyをparseする
- 結果はreq.bodyにCreateLogでセットされる
- IDが無い場合はSQLErrorを返す

---

### routes

routesは同様に

```
POST /log controllers.LogController.create()
```

CSRF違反で失敗するのでCSRFフィルターを一旦外す

application.conf

```
play.filters.csrf.header.bypassHeaders {
  X-Requested-With = "*"
}
```

---

## DELETE /log/:id

各自への自主課題とします

---

## 発展的な内容

- 集計してみる
- テスト書く(直す)
- 認証をする
- ScalikeJDBCでゴッツいSQL書く

余裕があったらやってみてください

完成サンプル

https://github.com/ponkotuy/get-started-scala
