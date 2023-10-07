import json
import sys

def main(argv):
    obj1 = {
        'name': 'DK Moon',
        'id': 12345678,
    }

    s = json.dumps(obj1)
    print(s)
    print('Type', type(s).__name__)

if __name__ == '__main__':
    main(sys.argv)