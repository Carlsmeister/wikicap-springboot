import re
from dataclasses import dataclass


@dataclass(frozen=True)
class WikiCleaner():
    """
    A class for cleaning and processing Wikipedia year summary text.
    """
    ref_tag: re.Pattern
    ref_self: re.Pattern
    html_tag: re.Pattern
    comments: re.Pattern
    templates: re.Pattern
    files: re.Pattern
    wiki_link: re.Pattern
    quotes: re.Pattern
    date_prefix: re.Pattern

    @staticmethod
    def build() -> "WikiCleaner":
        """
        Factory method to create a WikiCleaner instance with predefined regex patterns.
        """

        months_pattern = r"==\s*(January|February|March|April|May|June|July|August|September|October|November|December)"
        return WikiCleaner(
            ref_tag=re.compile(r"<ref[^>]*>.*?<\/ref>", re.DOTALL | re.IGNORECASE),
            ref_self=re.compile(r"<ref[^\/>]*/>", re.IGNORECASE),
            html_tag=re.compile(r"<[^>]+>", re.DOTALL),
            comments=re.compile(r"<!--.*?-->", re.DOTALL),
            templates=re.compile(r"\{\{[^}]+\}\}", re.DOTALL),
            files=re.compile(r"\[\[(File|Image):[^\]]+\]\]", re.IGNORECASE),
            wiki_link=re.compile(r"\[\[([^\]]+)\]\]"),
            quotes=re.compile(r"''+"),
            date_prefix=re.compile(
                rf"^(?:\[\[)?({months_pattern})\s+(\d{{1,2}})(?:\]\])?\s*[–—-]\s*",
                re.IGNORECASE,
            ),
        )

    def replace_wiki_link(self, match: re.Match) -> str:
        """
        Replacement function to extract the display text from a Wikipedia link.
        args:
            match (re.Match): The regex match object.
        """
        inner = (match.group(1) or "").strip()
        if not inner:
            return ""

        lowered = inner.lower()

        if lowered.startswith(("category:", "help:", "portal:", "special:")):
            return ""

        if "|" in inner:
            return inner.split("|")[-1].strip()

        return inner


    def clean_event_line(self, line: str, *, max_len: int = 200, keep_date_prefix: bool = True) -> str:
        """
        Cleans a single event line by removing unwanted patterns and truncating if necessary.
        args:
            line (str): The event line to clean.
            max_len (int): Maximum length of the cleaned line.
        returns: str: The cleaned event line.
        """

        stripped = line.strip()
        if not stripped.lstrip().startswith("*"):
            return ""

        stripped = stripped.lstrip("*").strip()

        prefix = ""
        date_match = self.date_prefix.match(stripped)
        if date_match:
            if keep_date_prefix:
                month = date_match.group(1).capitalize()
                day = date_match.group(2)
                prefix = f"{month[:3]} {day} - "
            stripped = self.date_prefix.sub("", stripped).strip()
        # Refs / HTML / Comments
        stripped = self.ref_tag.sub("", stripped)
        stripped = self.ref_self.sub("", stripped)
        stripped = self.comments.sub("", stripped)
        stripped = self.html_tag.sub("", stripped)

        # Templates / Files
        stripped = self.files.sub("", stripped)
        stripped = self.templates.sub("", stripped)

        # Wiki Links / Plain Text
        stripped = self.wiki_link.sub(self.replace_wiki_link, stripped)

        # Bold / Italics
        stripped = self.quotes.sub("", stripped)

        # Normalize dashes and whitespace
        stripped = stripped.replace("–", "-").replace("—", "-")
        stripped = re.sub(r"\s+", " ", stripped).strip(" ---")

        #Filter trash Lol
        if not stripped or len(stripped) < 8:
            return ""

        stripped = (prefix + stripped).strip()

        #Trim
        if len(stripped) > max_len:
            stripped = stripped[: max_len - 1].rstrip() + "..."
        return stripped

CLEANER = WikiCleaner.build()

