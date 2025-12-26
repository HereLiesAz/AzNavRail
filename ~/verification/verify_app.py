from playwright.sync_api import sync_playwright, expect
import time

def verify_app():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # Give the server a moment to start
        time.sleep(2)

        try:
            page.goto("http://localhost:5173")

            # 1. Verify Home Page and Buttons
            expect(page.get_by_text("Welcome to the AzNavRail Web Demo")).to_be_visible()
            expect(page.get_by_role("button", name="Click Me")).to_be_visible()

            # 2. Verify Navigation Rail Expansion
            # Click header to expand
            page.locator(".header").click()
            expect(page.locator(".az-nav-rail.expanded")).to_be_visible()

            # 3. Verify Hierarchical Navigation
            # Click "Features" host item
            page.get_by_text("Features").click()
            # Expect sub-items to appear
            expect(page.get_by_text("Buttons")).to_be_visible()
            expect(page.get_by_text("Inputs")).to_be_visible()

            # 4. Navigate to Buttons Page
            page.get_by_text("Buttons").click()
            expect(page.get_by_role("heading", name="Buttons")).to_be_visible()

            # 5. Verify Toggles and Cyclers
            expect(page.get_by_text("Toggle is OFF")).to_be_visible()
            page.locator(".az-toggle-container").click()
            expect(page.get_by_text("Toggle is ON")).to_be_visible()

            expect(page.get_by_text("Cycler:")).to_be_visible()

            # 6. Navigate to Inputs
            page.locator(".header").click() # Expand
            page.get_by_text("Features").click() # Expand group
            page.get_by_text("Inputs").click()
            expect(page.get_by_role("heading", name="Inputs")).to_be_visible()

            # 7. Screenshot
            page.screenshot(path="/home/jules/verification/aznavrail_web_verification.png")
            print("Verification successful!")

        except Exception as e:
            print(f"Verification failed: {e}")
            page.screenshot(path="/home/jules/verification/failure.png")
            raise e
        finally:
            browser.close()

if __name__ == "__main__":
    verify_app()
