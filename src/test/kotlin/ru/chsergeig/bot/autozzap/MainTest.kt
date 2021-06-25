package ru.chsergeig.bot.autozzap

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.platform.commons.logging.Logger
import org.junit.platform.commons.logging.LoggerFactory
import org.openqa.selenium.By
import org.openqa.selenium.Platform
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.Select
import java.net.URL
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MainTest {

    private val log: Logger = LoggerFactory.getLogger(MainTest::class.java)

    private val currents: List<String> = listOf(
            "Выберите расписание",
            "02 Марта 2020-08 Марта 2020",
            "09 Марта 2020-15 Марта 2020",
            "16 Марта 2020-22 Марта 2020",
            "23 Марта 2020-29 Марта 2020",
            "15 Июня 2020-22 Июня 2020",
            "22 Июня 2020-28 Июня 2020",
            "29 Июня 2020-05 Июля 2020",
            "06 Июля 2020-12 Июля 2020",
            "13 Июля 2020-19 Июля 2020",
            "20 Июля 2020-26 Июля 2020",
            "27 Июля 2020-02 Августа 2020",
            "03 Августа 2020-09 Августа 2020",
            "10 Августа 2020-16 Августа 2020",
            "17 Августа 2020-23 Августа 2020",
            "24 Августа 2020-30 Августа 2020",
            "31 Августа 2020-05 Сентября 2020",
            "07 Сентября 2020-13 Сентября 2020",
            "14 Сентября 2020-20 Сентября 2020",
            "21 Сентября 2020-27 Сентября 2020"
    )

    private var driver: RemoteWebDriver? = null

    @BeforeAll
    fun initDriver() {
        log.info { "Creating driver" }
        val capabilities = DesiredCapabilities("chrome", "83.0.4103.61", Platform.LINUX)
        capabilities.setCapability("enableVNC", false)
        capabilities.setCapability("timeZone", "EU/Moscow")
//        driver = wrapOperation { ChromeDriver() }
        driver = wrapOperation { RemoteWebDriver(URL("http://localhost:4444/wd/hub"), capabilities) }
        log.info { "Driver created" }
    }

    @AfterAll
    fun terminateDriver() {
        log.warn { "Closing driver" }
        driver!!.close()
        driver!!.quit()
    }

    @Test
    fun doCheck() {
        driver!!.get("http://lk.ucavtostart.ru/component/school/")
        log.info { "Start page opened" }
        val loginInput = wrapOperation { driver!!.findElement(By.id("modlgn_username")) }
        val passwdInput = wrapOperation { driver!!.findElement(By.id("modlgn_passwd")) }
        val loginButton = wrapOperation { driver!!.findElement(By.name("Submit")) }
        wrapOperation { loginInput.clear() }
        wrapOperation { loginInput.sendKeys(System.getenv()["ZZ_USER_NAME"]) }
        log.info { "Login entered" }
        wrapOperation { passwdInput.clear() }
        wrapOperation { passwdInput.sendKeys(System.getenv()["ZZ_USER_PASSWORD"]) }
        log.info { "Pass entered" }
        wrapOperation { loginButton.click() }
        log.info { "Login button clicked" }
        val timesheetLink = wrapOperation { driver!!.findElement(By.xpath("//*[@id='ja-col1']//*[text()='Расписание занятий']")) }
        wrapOperation { timesheetLink.click() }
        log.info { "Timesheet link clicked" }
        val periodSelector = wrapOperation { Select(driver!!.findElement(By.xpath("//*[@id='raspisanie_select']"))) }
        val periodOptions: List<WebElement> = wrapOperation { driver!!.findElements(By.xpath("//*[@id='raspisanie_select']/option")) }
        val values = wrapOperation { periodOptions.associateBy({ it.getAttribute("value") }, { it.text }) }
        val newValues = values.entries.filter { !currents.contains(it.value) }
        log.info {
            """
            |Got entries:
            |$values
            |New entries:
            |$newValues
        """.trimMargin("|")
        }
        if (newValues.isEmpty()) {
            log.warn { "No new values in ddl" }
            throw RuntimeException("No new values in ddl")
        }
        if (newValues.size > 1) {
            log.warn { "Too much new values in ddl" }
            throw RuntimeException("Too much new values in ddl")
        }
        wrapOperation { periodSelector.selectByValue(newValues[0].key) }
        log.info { "New entry selected" }

        val successes: List<String> = listOf(
                selectInTable(8, 6),
                selectInTable(9, 6),
                selectInTable(8, 7),
                selectInTable(9, 7))
        if (!successes.contains(System.getenv()["ZZ_USER_FIO"])) {
            log.warn { "Failed to queue" }
            throw RuntimeException("Failed to queue")
        }

    }

    private fun selectInTable(row: Int, column: Int): String {
        log.info { "Looking for entry {row=$row column=$column}" }
        val table = wrapOperation { driver!!.findElement(By.xpath("//table")) }
        log.info { "Table is here" }
        val tableRow = wrapOperation { table.findElements(By.xpath(".//*[@class='users_polosa']"))[row - 1] }
        log.info { "Got row $row" }
        val tableCell = wrapOperation { tableRow.findElements(By.xpath(".//td"))[column - 1] }
        log.info { "Got column $column" }
        if (wrapOperation { tableCell.text }.contains(System.getenv()["ZZ_USER_FIO"]!!)) {
            return System.getenv()["ZZ_USER_FIO"]!!
        }
        log.info { "Obtained cell" }
        wrapOperation {
            Actions(driver)
                    .moveToElement(tableCell)
                    .build()
                    .perform()
        }
        log.info { "Mouseover succeed" }
        try {
            log.info { "Try to find plus icon" }
            val plusIcon = wrapOperation { driver!!.findElement(By.xpath(".//*[contains(@class, 'edit') and contains(@class, 'iconki')]")) }
            wrapOperation { plusIcon.click() }
            log.warn { "Plus icon clicked" }
        } catch (t: Throwable) {
            log.error { t.message }
        }
        return wrapOperation { tableCell.text }
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
            TimeUnit.MILLISECONDS.sleep(50)
        }
        log.warn { "Failed after $counter attempts. ${ett.localizedMessage}" }
        throw RuntimeException("Failed after $counter attempts", ett)
    }

}
