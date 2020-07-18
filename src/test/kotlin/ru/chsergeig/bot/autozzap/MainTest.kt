package ru.chsergeig.bot.autozzap

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
            "20 Июля 2020-26 Июля 2020"
    )

    var driver: RemoteWebDriver? = null

    @BeforeAll
    fun initDriver() {
        val capabilities = DesiredCapabilities("chrome", "83.0.4103.61", Platform.LINUX)
        capabilities.setCapability("enableVNC", false);
        capabilities.setCapability("timeZone", "EU/Moscow");
        driver = RemoteWebDriver(URL("http://localhost:4444/wd/hub"), capabilities)
    }

    @AfterAll
    fun terminateDriver() {
        driver!!.close()
        driver!!.quit()
    }


    @Test
    fun doCheck() {
        driver!!.get("http://lk.ucavtostart.ru/component/school/")
        println("Start page opened")
        val loginInput = driver!!.findElement(By.id("modlgn_username"))
        val passwdInput = driver!!.findElement(By.id("modlgn_passwd"))
        val loginButton = driver!!.findElement(By.name("Submit"))
        TimeUnit.MILLISECONDS.sleep(500)
        loginInput.clear()
        TimeUnit.MILLISECONDS.sleep(500)
        loginInput.sendKeys(System.getenv()["ZZ_USER_NAME"])
        TimeUnit.MILLISECONDS.sleep(500)
        println("Login entered")
        passwdInput.clear()
        TimeUnit.MILLISECONDS.sleep(500)
        passwdInput.sendKeys(System.getenv()["ZZ_USER_PASSWORD"])
        TimeUnit.MILLISECONDS.sleep(500)
        println("Pass entered")
        loginButton.click()
        println("Login button clicked")
        TimeUnit.MILLISECONDS.sleep(500)
        val timesheetLink = driver!!.findElement(By.xpath("//*[@id='ja-col1']//*[text()='Расписание занятий']"))
        TimeUnit.MILLISECONDS.sleep(500)
        timesheetLink.click()
        println("Timesheet link clicked")
        TimeUnit.MILLISECONDS.sleep(500)
        val periodSelector = Select(driver!!.findElement(By.id("raspisanie_select")))
        val periodOptions: List<WebElement> = driver!!.findElements(By.xpath("//*[@id='raspisanie_select']/option"))
        val values = periodOptions.associateBy({ it.getAttribute("value") }, { it.text })
        val newEntries = values.entries.filter { !currents.contains(it.value) }
        println("Got entries. New entries: $newEntries")
        if (newEntries.isEmpty()) {
            System.err.println("No new values in ddl")
            throw RuntimeException("No new values in ddl")
        }
        if (newEntries.size > 1) {
            System.err.println("Too much new values in ddl")
            throw RuntimeException("Too much new values in ddl")
        }
        periodSelector.selectByValue(newEntries[0].key)
        println("New entry selected")
        TimeUnit.MILLISECONDS.sleep(500)
        val successes: List<String> = listOf(
                selectInTable(8, 6),
                selectInTable(9, 6),
                selectInTable(8, 7),
                selectInTable(9, 7))
        if (!successes.contains(System.getenv()["ZZ_USER_FIO"])) {
            System.err.println("Failed to queue")
            throw  RuntimeException("Failed to queue")
        }
    }

    private fun selectInTable(row: Int, column: Int): String {
        TimeUnit.MILLISECONDS.sleep(500)
        println("Looking for entry {row=$row column=$column}")
        val table = driver!!.findElement(By.xpath("//table"))
        println("Table is here")
        val tableRow = table.findElements(By.xpath(".//*[@class='users_polosa']"))[row - 1]
        println("Got row")
        val tableCell = tableRow.findElements(By.xpath(".//td"))[column - 1]
        println("Obtained cell")
        Actions(driver)
                .moveToElement(tableCell)
                .build()
                .perform()
        println("Mouseover succeed")
        try {
            println("Try to find plus icon")
            val plusIcon = driver!!.findElement(By.xpath(".//*[contains(@class, 'edit') and contains(@class, 'iconki')]"))
            plusIcon.click()
            println("Plus icon clicked")
        } catch (ignore: org.openqa.selenium.NoSuchElementException) {
        }
        TimeUnit.MILLISECONDS.sleep(100)
        return tableCell.text
    }

}