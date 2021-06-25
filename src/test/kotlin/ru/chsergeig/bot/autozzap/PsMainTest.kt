package ru.chsergeig.bot.autozzap

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.platform.commons.logging.Logger
import org.junit.platform.commons.logging.LoggerFactory
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.remote.RemoteWebDriver
import java.io.File
import java.lang.Thread.sleep
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PsMainTest {

    private val log: Logger = LoggerFactory.getLogger(PsMainTest::class.java)

    private lateinit var driver: RemoteWebDriver

    @BeforeAll
    fun initDriver() {
        System.setProperty(
            "webdriver.chrome.driver",
            File(ClassLoader.getSystemResource("chromedriver.exe").toURI()).toString()
        )
        log.info { "Creating driver" }
        val options = ChromeOptions()
        driver = wrapOperation { ChromeDriver(options) }
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS)
        driver.manage().window().maximize()
        log.info { "Driver created" }
    }

    @AfterAll
    fun terminateDriver() {
        log.warn { "Not closing driver to not loose session" }
    }

    @Test
    fun doCheck() {
        driver.get("https://www.dns-shop.ru/product/2645e72c6fca1b80/igrovaa-konsol-playstation-5/")
        log.info { "Start page opened" }
        val loginButton = wrapOperation { driver.findElement(By.cssSelector("[data-role='login-button']")) }
        wrapOperation { loginButton.click() }
        val loginWithPassButton =
            wrapOperation { driver.findElement(By.cssSelector(".block-other-login-methods__password-caption")) }
        wrapOperation { loginWithPassButton.click() }
        val usernameInput =
            wrapOperation { driver.findElement(By.xpath("//*[@class='base-ui-input-row__label'][text()='Телефон или e-mail']/../input")) }

        wrapOperation { Actions(driver).moveToElement(usernameInput, 10, 10).click().build().perform() }
        wrapOperation { Actions(driver).sendKeys(System.getProperty("ZZ_USER_LOGIN")).build().perform() }

        val passInput =
            wrapOperation { driver.findElement(By.xpath("//*[@class='base-ui-input-row__label'][text()='Пароль']/../input")) }

        wrapOperation { Actions(driver).moveToElement(passInput, 10, 10).click().build().perform() }
        wrapOperation { Actions(driver).sendKeys(System.getProperty("ZZ_USER_PASSWORD")).build().perform() }

        val enterButton =
            wrapOperation { driver.findElement(By.xpath("//*[@class='form-entry-with-password']//*[text()='Войти']")) }

        wrapOperation { enterButton.click() }

        while (true) {
            try {
                wrapOperation { driver.findElement(By.xpath("//*[@*='product-card-top__buy']//*[text()='Товар недоступен для продажи']")) }
                val ms = (40_000L * (1 + Math.random())).toLong()
                log.warn { "Sleeping for $ms ms until ${Date.from(Instant.now().plusMillis(ms))}" }
                sleep(ms)
                driver.navigate().refresh()
            } catch (nme: NoSuchElementException) {
                break
            }
        }
    }

    private fun <T> wrapOperation(supplier: () -> T): T {
        val deadLine = System.nanoTime() + 10_000_000_000L
        var result: T
        var ett: Exception = RuntimeException()
        var counter = 0
        while (System.nanoTime() < deadLine) {
            try {
                counter++
                result = supplier.invoke()
                return result
            } catch (e: Exception) {
                ett = e
            }
            sleep(200)
        }
        log.warn { "Failed after $counter attempts. ${ett.localizedMessage}" }
        ett.addSuppressed(RuntimeException("Failed after $counter attempts"))
        throw ett
    }

}
