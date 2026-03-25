from typing import Dict, List
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick


class TradingStrategy:
    def __init__(self, name: str):
        self.name = name
        self.signals = []
    
    def analyze(self, tick: Tick, history: List[Tick]) -> Dict:
        raise NotImplementedError
