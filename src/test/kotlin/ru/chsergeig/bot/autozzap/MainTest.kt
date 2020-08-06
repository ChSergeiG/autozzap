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
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait
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
            "03 Августа 2020-09 Августа 2020"
    )

    var driver: RemoteWebDriver? = null

    @BeforeAll
    fun initDriver() {
        log.info { "Creating driver" }
        val capabilities = DesiredCapabilities("chrome", "83.0.4103.61", Platform.LINUX)
        capabilities.setCapability("enableVNC", false)
        capabilities.setCapability("timeZone", "EU/Moscow")
        driver = RemoteWebDriver(URL("http://localhost:4444/wd/hub"), capabilities)
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
        val loginInput = driver!!.findElement(By.id("modlgn_username"))
        val passwdInput = driver!!.findElement(By.id("modlgn_passwd"))
        val loginButton = driver!!.findElement(By.name("Submit"))
        WebDriverWait(driver, 10).until { ExpectedConditions.visibilityOf(loginInput) }
        loginInput.clear()
        loginInput.sendKeys(System.getenv()["ZZ_USER_NAME"])
        WebDriverWait(driver, 10).until { ExpectedConditions.visibilityOf(passwdInput) }
        log.info { "Login entered" }
        passwdInput.clear()
        passwdInput.sendKeys(System.getenv()["ZZ_USER_PASSWORD"])
        log.info { "Pass entered" }
        WebDriverWait(driver, 10).until { ExpectedConditions.visibilityOf(loginButton) }
        loginButton.click()
        log.info { "Login button clicked" }
        val timesheetLink = driver!!.findElement(By.xpath("//*[@id='ja-col1']//*[text()='Расписание занятий']"))
        WebDriverWait(driver, 10).until { ExpectedConditions.visibilityOf(timesheetLink) }
        timesheetLink.click()
        log.info { "Timesheet link clicked" }
        WebDriverWait(driver, 10).until { ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='raspisanie_select']")) }
        val periodSelector = Select(driver!!.findElement(By.xpath("//*[@id='raspisanie_select']")))
        val periodOptions: List<WebElement> = driver!!.findElements(By.xpath("//*[@id='raspisanie_select']/option"))
        val values = periodOptions.associateBy({ it.getAttribute("value") }, { it.text })
        val newValues = values.entries.filter { !currents.contains(it.value) }
        log.info { "Got entries:\n$values.\nNew entries: $newValues" }
        if (newValues.isEmpty()) {
            log.warn { "No new values in ddl" }
            throw RuntimeException("No new values in ddl")
        }
        if (newValues.size > 1) {
            log.warn { "Too much new values in ddl" }
            throw RuntimeException("Too much new values in ddl")
        }
        periodSelector.selectByValue(newValues[0].key)
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
        TimeUnit.MILLISECONDS.sleep(500)
        log.info { "Looking for entry {row=$row column=$column}" }
        val table = driver!!.findElement(By.xpath("//table"))
        WebDriverWait(driver, 10).until { ExpectedConditions.visibilityOf(table) }
        log.info { "Table is here" }
        val tableRow = table.findElements(By.xpath(".//*[@class='users_polosa']"))[row - 1]
        WebDriverWait(driver, 10).until { ExpectedConditions.visibilityOf(tableRow) }
        log.info { "Got row $row" }
        val tableCell = tableRow.findElements(By.xpath(".//td"))[column - 1]
        WebDriverWait(driver, 10).until { ExpectedConditions.visibilityOf(tableCell) }
        log.info { "Got column $column" }
        if (tableCell.text.contains(System.getenv()["ZZ_USER_FIO"]!!)) {
            return System.getenv()["ZZ_USER_FIO"]!!
        }
        log.info { "Obtained cell" }
        Actions(driver)
                .moveToElement(tableCell)
                .build()
                .perform()
        log.info { "Mouseover succeed" }
        try {
            log.info { "Try to find plus icon" }
            val plusIcon = driver!!.findElement(By.xpath(".//*[contains(@class, 'edit') and contains(@class, 'iconki')]"))
            WebDriverWait(driver, 10).until { ExpectedConditions.visibilityOf(plusIcon) }
            plusIcon.click()
            log.warn { "Plus icon clicked" }
        } catch (ignore: org.openqa.selenium.NoSuchElementException) {
        }
        return tableCell.text
    }

}
