import re
from dataclasses import dataclass


@dataclass(frozen=True)
class WikiCleaner():
    """
    Utility for cleaning and normalizing event lines extracted from Wikipedia wikitext.

    The cleaner removes references, HTML, templates, file links and formatting,
    and converts wiki links (e.g. [[Page|label]]) into human-readable text.

    Attributes:
        ref_tag: Regex for <ref> ... </ref> tags.
        ref_self: Regex for self-closing <ref/> tags.
        html_tag: Regex for HTML tags.
        comments: Regex for HTML comments <!-- ... -->.
        templates: Regex for template blocks {{ ... }}.
        files: Regex for [[File:...]] / [[Image:...]] links.
        wiki_link: Regex for wiki links [[...]].
        quotes: Regex for wiki bold/italic markers ('' and ''').
        date_prefix: Regex used to detect and optionally preserve "Month day - " prefix.
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
        Create a WikiCleaner instance with precompiled regex patterns.

        Returns:
            WikiCleaner: An instance of WikiCleaner with compiled regex patterns.
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
    Convert a wiki link match into display text.

    Examples:
        [[Stockholm]] -> "Stockholm"
        [[Stockholm|the capital]] -> "the capital"
        [[Category:Something]] -> "" (filtered out)

    Args:
        match: A regex match object from `self.wiki_link`.

    Returns:
        The cleaned display text for the link, or an empty string if the link
        points to a filtered namespace (Category/Help/Portal/Special).
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
        Clean a single bullet-point event line from wikipedia wikitext.

        This function expects Wikipedia-style bullet lines (starting with `*`).
        It optionally preserves a shortened date prefix (e.g. "Jan 12 - "),
        removes references/templates/markup, resolves wiki links to plain text,
        normalizes whitespace, and truncates overly long lines.

        Args:
            Line (str): The raw wikitext line to clean (typically starting with "*")
            max_len (int): Maximum length of the cleaned line (including prefix)
            keep_date_prefix (bool): Whether to preserve the "Mon DD - " date prefix if present

        Returns:
        A cleaned event string, or an empty string if:
        - the input is not a bullet line,
        - the cleaned result is too short / empty after filtering.

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

