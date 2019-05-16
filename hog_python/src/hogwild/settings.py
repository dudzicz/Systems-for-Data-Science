import os


LOCAL_PATH = '../../datasets/datasets/'

# If testing locally, use localhost:port with different ports for each node/coordinator
# When running on different machines, can use the same port for all.
N_WORKERS = '20'
RUNNING_WHERE = 'local'
DATA_PATH = LOCAL_PATH


coordinator_address = 'localhost:50051'
worker_addresses = ['localhost:50052', 'localhost:50053','localhost:50054', 'localhost:50055','localhost:50056','localhost:50057', 'localhost:50058','localhost:50059', 'localhost:50060','localhost:50061']

worker_addresses = worker_addresses + ['localhost:50062', 'localhost:50063','localhost:50064', 'localhost:50065','localhost:50066','localhost:50067', 'localhost:50068','localhost:50069', 'localhost:50070','localhost:50071']
port = 50051

TRAIN_FILE = os.path.join(DATA_PATH, 'lyrl2004_vectors_train.dat') if DATA_PATH else ''
TOPICS_FILE = os.path.join(DATA_PATH, 'rcv1-v2.topics.qrels')
TEST_FILES = [os.path.join(DATA_PATH, x) for x in ['lyrl2004_vectors_test_pt0.dat',
                                                   'lyrl2004_vectors_test_pt1.dat',
                                                   'lyrl2004_vectors_test_pt2.dat',
                                                   'lyrl2004_vectors_test_pt3.dat']]

running_mode = 'asynchronous'
synchronous = running_mode == 'synchronous'

subset_size = 256  # Number of datapoints to train on each epoch
# Learning rate for SGD. The term (100/subset_size) is used to adapt convergence to different subset sizes than 100
learning_rate = 0.03 * (100 / subset_size) / len(worker_addresses)
validation_split = 0.1  # Percentage of validation data
epochs = 10000 # Number of training iterations over subset on each node
persistence = 25  # Abort if after so many epochs learning rate does not decrease
lambda_reg = 1e-5  # Regularization parameter

#bash run.sh -n 3 -r synchronous -w cluster
