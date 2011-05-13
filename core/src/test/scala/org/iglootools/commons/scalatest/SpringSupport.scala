package org.iglootools.commons.scalatest

import org.springframework.context.ApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
;
trait SpringSupport {
  val locations: Array[String]
  protected lazy val applicationContext: ApplicationContext = new ClassPathXmlApplicationContext(locations : _*)
  protected def getBean[T](clazz: Class[T]): T = applicationContext.getBean(clazz)

}