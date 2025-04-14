import requests
from firebase import firebase
from transformers import pipeline  # ✅ Added for summarization

# GNews API endpoint and key
GNEWS_API_KEY = "/api key"  # Get it from https://gnews.io/
GNEWS_URL = f"https://gnews.io/api/v4/top-headlines?country=in&lang=en&apikey={GNEWS_API_KEY}"

if GNEWS_API_KEY == "YOUR_GNEWS_API_KEY":
    print("ERROR: Please get your GNews API key from https://gnews.io/")
    print("1. Go to https://gnews.io/")
    print("2. Click 'Get API Key'")
    print("3. Sign up and get your API key")
    print("4. Replace 'YOUR_GNEWS_API_KEY' with your actual key")
    exit()

# ✅ Initialize BART summarizer
summarizer = pipeline("summarization", model="facebook/bart-large-cnn")

# ✅ Function to summarize long text
def summarize_text(text):
    if not text or len(text.strip()) < 30:
        return text
    try:
        summary = summarizer(text, max_length=60, min_length=25, do_sample=False)
        return summary[0]['summary_text']
    except Exception as e:
        print(f"Error summarizing text: {e}")
        return text

all_news_data = {}

try:
    # Get news from GNews
    print(f"Fetching news from GNews...")
    response = requests.get(GNEWS_URL)
    news_data = response.json()

    if 'articles' in news_data:
        articles = news_data['articles']
        i = 1

        for article in articles[:25]:  # Get top 25 articles
            try:
                description = article['description'] if 'description' in article else ''
                summarized_desc = summarize_text(description)  # ✅ Apply summarization

                all_news_data[i] = [
                    article['url'],  # newslink
                    article['image'] if 'image' in article else '',  # imagelink
                    article['title'],  # head
                    article['title'],  # title (same as head)
                    summarized_desc  # ✅ Store summarized description
                ]

                # Print the extracted data for debugging
                print(f"\nProcessing article {i}:")
                print(f"URL: {article['url']}")
                print(f"Image URL: {article['image'] if 'image' in article else 'No image'}")
                print(f"Headline: {article['title']}")
                print(f"Description: {summarized_desc[:600]}...")  # ✅ Show summarized text

                i += 1
            except Exception as e:
                print(f"Error processing article: {e}")
    else:
        print(f"Error from GNews: {news_data.get('message', 'Unknown error')}")
        print("Please check your API key at https://gnews.io/")

except Exception as e:
    print(f"Error fetching news: {e}")

print(f"\nTotal articles found: {len(all_news_data)}")

config = {
    "apiKey": "api key2",
    "authDomain": "news-app-e1742.firebaseapp.com",
    "databaseURL": "https://news-app-e1742-default-rtdb.firebaseio.com",
    "storageBucket": "news-app-e1742.firebasestorage.app"
}

# Create firebase database connection
firebaseconn = firebase.FirebaseApplication(config["databaseURL"], None)

# Try to authenticate with email and password if needed
try:
    auth = firebaseconn.auth()
    if auth:
        firebaseconn = firebase.FirebaseApplication(config["databaseURL"], authentication=auth)
except:
    print("Authentication not required or failed. Proceeding without authentication.")

for i in all_news_data:
    data = {
        "newslink": all_news_data[i][0],
        "imagelink": all_news_data[i][1],
        "head": all_news_data[i][2],
        "title": all_news_data[i][3],
        "desc": all_news_data[i][4]
    }

    try:
        result = firebaseconn.patch("/News/%s" % i, data)
        print(f"Successfully added news {i}")
    except Exception as e:
        print(f"Error adding news {i}: {e}")

print(f"\nTotal articles uploaded to Firebase: {len(all_news_data)}")
