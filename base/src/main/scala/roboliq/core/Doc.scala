package roboliq.core
import scala.reflect.BeanProperty


/**
 * Documentation for an object.
 * 
 * @param textShort Short plain text
 * @param mdShort Short markdown
 * @param mdLong Long markdown
 */
class Doc(
	val plainShort_? : Option[String] = None,
	val mdShort_? : Option[String] = None,
	val mdLong_? : Option[String] = None
)

/**
 * Documentation bean for an object
 */
class DocBean {
	/** Short plain text */
	@BeanProperty var plainShort: String = null;
	/** Short markdown */
	@BeanProperty var mdShort: String = null;
	/** Long markdown */
	@BeanProperty var mdLong: String = null;
}